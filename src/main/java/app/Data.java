package app;

import static app.utils.FileUtils.deleteFile;
import static app.utils.Json.JSON_MAPPER;
import static app.utils.Validation.validate;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isAbstract;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.walk;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.util.Arrays.stream;
import static java.util.Collections.synchronizedList;
import static java.util.function.Predicate.not;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.reflections.scanners.Scanners.SubTypes;

import jakarta.validation.constraints.NotEmpty;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.ThreadSafeProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

@SuperBuilder
@ToString
@Log4j2
public class Data {

    @NotEmpty
    private final String repositoryUri;
    @NotEmpty
    @ToString.Exclude
    private final String repositoryToken;
    @NotEmpty
    private final String repositoryBranch;


    @SneakyThrows
    @SuppressWarnings("resource")
    public List<Site> loadSites() {
        var git = getGit();
        var repositoryPath = git.getRepository().getWorkTree().toPath().toAbsolutePath();

        var sites = new ArrayList<Site>();
        for (var siteClass : getSiteClasses()) {
            var siteFilesRepoPath = getSiteFilesRepoPath(siteClass);
            var siteFilesPath = repositoryPath.resolve(siteFilesRepoPath);
            if (exists(siteFilesPath)) {
                var paths = walk(siteFilesPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .toList();
                for (var path : paths) {
                    var site = JSON_MAPPER.readValue(path.toFile(), siteClass);
                    sites.add(site);
                }
            }
        }
        return sites;
    }

    @SneakyThrows
    public void saveSites(List<? extends Site> sites) {
        var git = getGit();
        var repositoryPath = git.getRepository().getWorkTree().toPath().toAbsolutePath();

        var sitesPath = repositoryPath.resolve(SITES_REPO_PATH);
        deleteFile(sitesPath);

        for (var site : sites) {
            var siteFilesRepoPath = getSiteFilesRepoPath(site.getClass());
            var siteFilesPath = repositoryPath.resolve(siteFilesRepoPath);
            createDirectories(siteFilesPath);

            var siteFilePath = siteFilesPath.resolve(site.getAuth().getLogin() + ".json");
            JSON_MAPPER.writeValue(siteFilePath.toFile(), site);
        }

        call(git.add()
            .addFilepattern(SITES_REPO_PATH)
        );

        var status = call(git.status());
        if (status.isClean()) {
            return;
        }

        var now = Instant.now();
        var adjustedNow = now.minus(now.getNano(), NANOS);
        call(git.commit()
            .setMessage("Auto-commit sites data changes: " + adjustedNow)
        );

        call(git.push());
    }


    private static final String SITES_REPO_PATH = "sites";

    private static String getSiteFilesRepoPath(Class<? extends Site> siteClass) {
        return SITES_REPO_PATH + '/' + UPPER_CAMEL.to(LOWER_HYPHEN, siteClass.getSimpleName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private synchronized List<Class<? extends Site>> getSiteClasses() {
        if (_siteClasses == null) {
            var reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(Site.class.getProtectionDomain().getCodeSource().getLocation())
            );

            return _siteClasses = (List) reflections.get(SubTypes.of(Site.class).asClass()).stream()
                .filter(not(Class::isInterface))
                .filter(not(type -> isAbstract(type.getModifiers())))
                .peek(type -> {
                    var markerAnnotation = SiteMarker.class;
                    if (!type.isAnnotationPresent(markerAnnotation)) {
                        log.warn("{} is not annotated with {}", type, markerAnnotation);
                    }
                })
                .toList();
        }
        return _siteClasses;
    }

    @ToString.Exclude
    private List<Class<? extends Site>> _siteClasses;


    private static final List<Runnable> SHUTDOWN_ACTIONS = synchronizedList(new ArrayList<>());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (var action : SHUTDOWN_ACTIONS) {
                try {
                    action.run();
                } catch (Throwable exception) {
                    log.warn(exception, exception);
                }
            }
        }));
    }

    @SneakyThrows
    private synchronized Git getGit() {
        if (_git == null) {
            validate(this);

            var repositoryDir = createTempDirectory(this.getClass().getName()).toAbsolutePath().toFile();
            SHUTDOWN_ACTIONS.add(() -> deleteFile(repositoryDir));

            _git = call(Git.cloneRepository()
                .setDirectory(repositoryDir)
                .setURI(repositoryUri)
                .setBranch(repositoryBranch)
                .setNoTags()
            );

            SHUTDOWN_ACTIONS.add(0, () -> _git.close());
            SHUTDOWN_ACTIONS.add(0, () -> _git.getRepository().close());
        }
        return _git;
    }

    @ToString.Exclude
    private Git _git;
    @ToString.Exclude
    private File _repositoryDir;


    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @SneakyThrows
    private <Result, Command extends GitCommand<Result>> Result call(Command command) {
        if (command instanceof TransportCommand<?, ?> transportCommand) {
            transportCommand.setTimeout(toIntExact(TIMEOUT.toSeconds()));

            transportCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                "x-access-token",
                repositoryToken
            ));
        }

        var progressMonitorSetters = stream(command.getClass().getMethods())
            .filter(method -> method.getParameterCount() == 1)
            .filter(method -> method.getParameterTypes()[0] == ProgressMonitor.class)
            .filter(method -> method.getName().startsWith("set"))
            .toList();
        for (var method : progressMonitorSetters) {
            method.invoke(command, new ThreadSafeProgressMonitor(new LoggingProgressMonitor()));
        }

        return command.call();
    }


    private static class LoggingProgressMonitor implements ProgressMonitor {

        private static final Level LEVEL = DEBUG;
        private static final Duration DELAY = Duration.ofSeconds(1);

        private String title;
        private int totalWork;
        private Instant lastLog;
        private boolean loggedPercents;

        @Override
        public void start(int totalTasks) {
            // do nothing
        }

        @Override
        public void beginTask(String title, int totalWork) {
            log.log(LEVEL, title);
            this.title = title;
            this.totalWork = totalWork;
            this.lastLog = Instant.now();
        }

        @Override
        public void update(int completed) {
            if (completed >= totalWork) {
                if (loggedPercents) {
                    log.log(LEVEL, () -> format("%s: %3d%%", title, 100));
                }

            } else {
                var now = Instant.now();
                var shouldBeLogged = now.minus(DELAY).isAfter(lastLog);
                if (shouldBeLogged) {
                    var doubleCompleted = (double) completed;
                    var percents = (int) (100.0 * (doubleCompleted / totalWork));
                    log.log(LEVEL, () -> format("%s: %3d%%", title, percents));
                    lastLog = now;
                    loggedPercents = true;
                }
            }
        }

        @Override
        public void endTask() {
            update(totalWork);
            title = null;
            totalWork = 0;
            lastLog = null;
            loggedPercents = false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

    }

}

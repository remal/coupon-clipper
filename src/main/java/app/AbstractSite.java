package app;

import static app.utils.ValidationUtils.validate;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static org.openqa.selenium.chrome.ChromeDriverLogLevel.INFO;
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL;

import app.utils.ExtendedElementWait;
import app.utils.ExtendedWebDriverWait;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.logging.log4j.LogManager;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.InvalidCookieDomainException;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.DefaultRecordingFileFactory;
import org.testcontainers.containers.VncRecordingContainer.VncRecordingFormat;
import org.testcontainers.lifecycle.TestDescription;

@Getter
@ToString
@SuperBuilder
public abstract class AbstractSite implements Site {

    protected abstract void clipCouponsImpl(RemoteWebDriver webDriver, ExtendedWebDriverWait wait);

    protected String canonizeUrl(String url) {
        return url.replace("://www.", "://");
    }


    @NotNull
    @Valid
    private final Auth auth;

    @Default
    @NotNull
    private final ArrayList<Cookie> cookies = new ArrayList<>();

    @Default
    @NotNull
    private final AtomicReference<Instant> lastCookieClean = new AtomicReference<>(Instant.now());


    @Override
    public final void clipCoupons() {
        LogManager.getLogger(this.getClass()).info("Clipping coupons for: {}", getAuth().getLogin());

        validate(this, "Validation failed for object of " + this.getClass());

        forBrowserContainer(browserContainer -> {
            var driver = browserContainer.getWebDriver();
            var wait = new ExtendedWebDriverWait(driver, this::canonizeUrl);

            //driver.manage().window().setSize(new Dimension(1680, 1050));

            clearCookiesIfNeeded();
            setCookiesTo(driver);

            try {
                clipCouponsImpl(driver, wait);

            } catch (Throwable exception) {
                getCookies().clear();
                lastCookieClean.set(Instant.now());
                throw exception;
            }

            setCookiesFrom(driver);
        });
    }

    private void clearCookiesIfNeeded() {
        var now = Instant.now();
        now = now.minus(now.getNano(), NANOS);
        var expirationSeconds = ThreadLocalRandom.current().nextLong(
            COOKIE_EXPIRATION_MIN.toSeconds(),
            COOKIE_EXPIRATION_MAX.toSeconds()
        );
        var cookieClean = now.minus(expirationSeconds, SECONDS);
        if (cookieClean.isAfter(lastCookieClean.get())) {
            getCookies().clear();
            lastCookieClean.set(now);
        }
    }

    private static final Duration COOKIE_EXPIRATION_MIN = Duration.ofDays(1);
    private static final Duration COOKIE_EXPIRATION_MAX = Duration.ofDays(14);

    private void setCookiesTo(RemoteWebDriver driver) {
        getCookies().stream()
            .filter(cookie -> cookie.getDomain() != null)
            .collect(groupingBy(cookie -> {
                var domain = cookie.getDomain();
                while (domain.startsWith(".")) {
                    domain = domain.substring(1);
                }
                return domain;
            }))
            .forEach((domain, domainCookies) -> {
                driver.get("http://" + domain + "/favicon.ico");

                for (var cookie : domainCookies) {
                    try {
                        driver.manage().addCookie(cookie);
                    } catch (InvalidCookieDomainException ignored) {
                        // do nothing
                    }
                }
            });
    }

    private void setCookiesFrom(RemoteWebDriver driver) {
        getCookies().clear();

        driver.manage().getCookies().stream()
            .filter(cookie -> cookie.getDomain() != null)
            .sorted(comparing(Cookie::getDomain)
                .thenComparing(Cookie::getName)
            )
            .forEach(getCookies()::add);
    }


    private static final File VNC_RECORDING_DIRECTORY = new File(".recordings").getAbsoluteFile();

    private static final MutableCapabilities SELENIUM_CAPABILITIES = new ChromeOptions()
        .setLogLevel(INFO);

    @SneakyThrows
    @SuppressWarnings("resource")
    private void forBrowserContainer(BrowserContainerAction action) {
        var browserContainer = new BrowserWebDriverContainer<>()
            .withRecordingMode(RECORD_ALL, VNC_RECORDING_DIRECTORY)
            .withCapabilities(SELENIUM_CAPABILITIES)
            .withRecordingFileFactory(new SiteRecordingFileFactory());
        try (browserContainer) {
            browserContainer.start();

            Throwable exception = null;
            try {
                action.execute(browserContainer);
            } catch (Throwable e) {
                exception = e;
            }

            try {
                browserContainer.afterTest(
                    new TestDescription() {
                        @Override
                        public String getFilesystemFriendlyName() {
                            return AbstractSite.this.getClass().getSimpleName();
                        }

                        @Override
                        public String getTestId() {
                            throw new UnsupportedOperationException();
                        }
                    },
                    Optional.ofNullable(exception)
                );
            } catch (Throwable e) {
                if (exception != null) {
                    e.addSuppressed(exception);
                }
                throw e;
            }

            if (exception != null) {
                throw exception;
            }
        }
    }

    @FunctionalInterface
    private interface BrowserContainerAction {
        void execute(BrowserWebDriverContainer<?> browserContainer) throws Throwable;
    }

    private static class SiteRecordingFileFactory extends DefaultRecordingFileFactory {
        @Override
        @SneakyThrows
        public File recordingFileForTest(
            File vncRecordingDirectory,
            String prefix,
            boolean succeeded,
            VncRecordingFormat recordingFormat
        ) {
            vncRecordingDirectory = vncRecordingDirectory.getAbsoluteFile();
            createDirectories(vncRecordingDirectory.toPath());

            return new File(vncRecordingDirectory, format(
                "%s-%s.%s",
                prefix,
                new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()),
                recordingFormat.getFilenameExtension()
            ));
        }
    }


    protected static ExtendedElementWait await(WebElement element) {
        return new ExtendedElementWait(element);
    }

}

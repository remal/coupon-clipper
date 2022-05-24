package app;

import static java.lang.System.getenv;
import static java.util.function.Predicate.not;

import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;

@Log4j2
public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        var data = Data.builder()
            .repositoryUri(getenv("DATA_REPOSITORY"))
            .repositoryToken(getenv("DATA_REPOSITORY_TOKEN"))
            .repositoryBranch(Optional.ofNullable(getenv("DATA_REPOSITORY_BRANCH"))
                .filter(not(String::isEmpty))
                .orElse("main")
            )
            .build();

        var sites = data.loadSites();

        var aggregatorException = new RuntimeException("Clipping coupons raised some exceptions");
        for (var site : sites) {
            try {
                site.clipCoupons();
            } catch (Throwable e) {
                aggregatorException.addSuppressed(e);
            }
        }

        data.saveSites(sites);

        if (aggregatorException.getSuppressed().length != 0) {
            throw aggregatorException;
        }
    }

    static {
        Log4jBridgeHandler.install(true, null, true);
    }

}

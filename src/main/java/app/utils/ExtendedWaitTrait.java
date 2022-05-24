package app.utils;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import lombok.SneakyThrows;

interface ExtendedWaitTrait {

    Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    Duration SLEEP_TIMEOUT = Duration.ofMillis(500);

    Duration RANDOM_SLEEP_MIN = Duration.ofMillis(1_000);
    Duration RANDOM_SLEEP_MAX = Duration.ofMillis(5_000);

    @SneakyThrows
    default void randomDuration() {
        var millis = ThreadLocalRandom.current().nextLong(
            RANDOM_SLEEP_MIN.toMillis(),
            RANDOM_SLEEP_MAX.toMillis()
        );
        Thread.sleep(millis);
    }

}

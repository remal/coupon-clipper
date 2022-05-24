package app.utils;

import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.util.function.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ExtendedWebDriverWait extends WebDriverWait implements ExtendedWaitTrait {

    private final WebDriver driver;
    private final Function<String, String> defaultUrlCanonizer;

    public ExtendedWebDriverWait(WebDriver driver, Function<String, String> defaultUrlCanonizer) {
        super(driver, WAIT_TIMEOUT, SLEEP_TIMEOUT);
        this.driver = driver;
        this.defaultUrlCanonizer = defaultUrlCanonizer;
    }

    @Override
    public <V> V until(Function<? super WebDriver, V> isTrue) {
        try {
            return super.until(isTrue);
        } finally {
            randomDuration();
        }
    }

    public WebElement untilVisible(By locator) {
        return until(visibilityOfElementLocated(locator));
    }

    public void untilUrlIs(Function<String, String> urlCanonizer, String url) {
        var urlCanonized = urlCanonizer.apply(url);

        until(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver ignored) {
                var currentUrlCanonized = urlCanonizer.apply(driver.getCurrentUrl());
                return currentUrlCanonized.equals(urlCanonized);
            }

            @Override
            public String toString() {
                return "current URL is " + url;
            }
        });
    }

    public void untilUrlIs(String url) {
        untilUrlIs(defaultUrlCanonizer, url);
    }

    public void untilUrlIsNot(Function<String, String> urlCanonizer, String url) {
        var urlCanonized = urlCanonizer.apply(url);

        until(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver ignored) {
                var currentUrlCanonized = urlCanonizer.apply(driver.getCurrentUrl());
                return !currentUrlCanonized.equals(urlCanonized);
            }

            @Override
            public String toString() {
                return "current URL is NOT " + url;
            }
        });
    }

    public void untilUrlIsNot(String url) {
        untilUrlIsNot(defaultUrlCanonizer, url);
    }

}

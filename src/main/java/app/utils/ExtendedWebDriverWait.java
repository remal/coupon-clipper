package app.utils;

import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.util.function.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ExtendedWebDriverWait extends WebDriverWait implements ExtendedWaitTrait {

    private final WebDriver driver;

    public ExtendedWebDriverWait(WebDriver driver) {
        super(driver, WAIT_TIMEOUT, SLEEP_TIMEOUT);
        this.driver = driver;
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

    public void untilUrlIsNot(String url) {
        until(__ -> !driver.getCurrentUrl().equals(url));
    }

}

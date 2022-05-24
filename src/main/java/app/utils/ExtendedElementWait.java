package app.utils;

import java.util.function.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

public class ExtendedElementWait extends FluentWait<WebElement> implements ExtendedWaitTrait {

    public ExtendedElementWait(WebElement element) {
        super(element);
        withTimeout(WAIT_TIMEOUT);
        pollingEvery(SLEEP_TIMEOUT);
        ignoring(NotFoundException.class);
    }

    @Override
    public <V> V until(Function<? super WebElement, V> isTrue) {
        try {
            return super.until(isTrue);
        } finally {
            randomDuration();
        }
    }

    public WebElement forVisibilityOfElementLocatedBy(By locator) {
        return until(new Function<>() {
            @Override
            public WebElement apply(WebElement element) {
                var resultElement = element.findElement(locator);
                return resultElement.isDisplayed() ? resultElement : null;
            }

            @Override
            public String toString() {
                return "visibility of child element located by " + locator;
            }
        });
    }

}

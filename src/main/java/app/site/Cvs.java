package app.site;

import static org.openqa.selenium.By.cssSelector;

import app.AbstractSite;
import app.DisabledSite;
import app.SiteMarker;
import app.utils.ExtendedWebDriverWait;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

@Value
@Jacksonized
@SuperBuilder
@Log4j2
@SiteMarker
public class Cvs extends AbstractSite implements DisabledSite {

    private static final String OFFERS_URL = "https://www.cvs.com/extracare/home";

    @Override
    @SneakyThrows
    protected void clipCouponsImpl(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        webDriver.get(OFFERS_URL);
        wait.randomDuration();

        WebElement signInButton;
        try {
            signInButton = webDriver.findElement(cssSelector(".sign-in-block .sign-in"));
        } catch (NotFoundException ignored) {
            log.debug("Already signed in");
            signInButton = null;
        }
        if (signInButton != null && signInButton.isDisplayed() && signInButton.isEnabled()) {
            log.debug("Signing in");
            signInButton.click();

            wait.untilVisible(cssSelector("#login-container #emailField")).sendKeys(getAuth().getLogin());
            wait.untilVisible(cssSelector("#login-container .cvs-checkbox-wrapper label")).click();
            wait.untilVisible(cssSelector("#login-container button.primary")).click();

            wait.untilVisible(cssSelector("#login-container #cvs-password-field-input")).sendKeys(getAuth().getLogin());
            wait.untilVisible(cssSelector("#login-container button.primary")).click();

            wait.untilUrlIs(OFFERS_URL);
        }

        wait.untilVisible(cssSelector("#sendAllCouponsToCard button.button-primary-normal")).click();

        var containersSelector = cssSelector(".coupon-box");
        var prevCouponItemsCounter = new AtomicInteger(
            webDriver.findElements(containersSelector).size()
        );
        while (true) {
            log.debug("Loading more coupons");
            webDriver.executeScript("window.scrollTo(0, document.body.scrollHeight)");

            try {
                wait.until(__ -> {
                    var couponItemsCounter = webDriver.findElements(containersSelector).size();
                    if (couponItemsCounter > prevCouponItemsCounter.get()) {
                        prevCouponItemsCounter.set(couponItemsCounter);
                        return true;
                    }
                    return false;
                });
            } catch (TimeoutException e) {
                break;
            }
        }
        webDriver.executeScript("window.scrollTo(0, -document.body.scrollHeight)");

        var containers = webDriver.findElements(containersSelector);
        for (var container : containers) {
            final WebElement button;
            try {
                button = container.findElement(cssSelector("button.action-items"));
                if (!button.isDisplayed() || !button.isEnabled()) {
                    continue;
                }
            } catch (NotFoundException ignored) {
                continue;
            }

            var title = container.findElement(cssSelector(".description-button"))
                .getText()
                .trim();
            log.info("Clipping Coupon: {}", title);

            button.click();

            await(container).forVisibilityOfElementLocatedBy(cssSelector("div.action-items"));
        }
    }

}

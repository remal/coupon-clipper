package app.site;

import static org.openqa.selenium.By.cssSelector;

import app.AbstractSite;
import app.DisabledSite;
import app.SiteMarker;
import app.utils.ExtendedWebDriverWait;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

@Value
@Jacksonized
@SuperBuilder
@Log4j2
@SiteMarker
public class Target extends AbstractSite implements DisabledSite {

    private static final String OFFERS_URL = "https://www.target.com/circle/offers";

    @Override
    protected void clipCouponsImpl(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        webDriver.get(OFFERS_URL);

        WebElement signInButton;
        try {
            signInButton = webDriver.findElement(cssSelector("[data-test=featured-offers-sign-in] button"));
        } catch (NotFoundException ignored) {
            // already signed in
            signInButton = null;
        }
        if (signInButton != null && signInButton.isDisplayed() && signInButton.isEnabled()) {
            signInButton.click();

            wait.untilVisible(cssSelector("[name=username]")).sendKeys(getAuth().getLogin());
            wait.untilVisible(cssSelector("[name=password]")).sendKeys(getAuth().getPassword());
            wait.untilVisible(cssSelector("[for=keepMeSignedIn]")).click();
            wait.untilVisible(cssSelector("[type=submit]")).click();

            wait.untilUrlIs(OFFERS_URL);
        }

        webDriver.executeScript(
            "document.querySelectorAll('#@web/component-header')"
                + ".forEach(element => element.remove())"
        );

        var containersSelector = cssSelector(".offer-grid-card > .offer-card");
        var loadMoreSelector = cssSelector(".h-text-center button");
        var loadMore = wait.untilVisible(loadMoreSelector);
        var prevCouponItemsCounter = new AtomicInteger(
            webDriver.findElements(containersSelector).size()
        );
        while (loadMore.isDisplayed()) {
            loadMore.click();
            wait.until(__ -> {
                var couponItemsCounter = webDriver.findElements(containersSelector).size();
                if (couponItemsCounter > prevCouponItemsCounter.get()) {
                    prevCouponItemsCounter.set(couponItemsCounter);
                    return true;
                }
                return false;
            });

            try {
                loadMore = webDriver.findElement(loadMoreSelector);
            } catch (NotFoundException ignored) {
                break;
            }

            wait.randomDuration();
        }
        webDriver.executeScript("window.scrollTo(0, -document.body.scrollHeight)");

        var containers = webDriver.findElements(containersSelector);
        for (var container : containers) {
            final WebElement button;
            try {
                button = container.findElement(cssSelector("[data-test=button-default]"));
                if (!button.isDisplayed() || !button.isEnabled()) {
                    continue;
                }
            } catch (NotFoundException ignored) {
                continue;
            }

            var title = container.findElement(cssSelector("[data-test=offer-title]"))
                .getText()
                .trim();
            log.info("Clipping Coupon: {}", title);

            button.click();

            await(container).forVisibilityOfElementLocatedBy(cssSelector("[data-test=button-confirmed]"));
        }
    }

}

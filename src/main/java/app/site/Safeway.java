package app.site;

import static org.openqa.selenium.By.cssSelector;

import app.AbstractSite;
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
public class Safeway extends AbstractSite {

    @Override
    protected void clipCouponsImpl(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        signIn(webDriver, wait);
        clickClipCouponsButtons(webDriver, wait);
    }

    private void signIn(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        var signInUrl = "https://safeway.com/account/sign-in.html";
        webDriver.get(signInUrl);

        if (!canonizeUrl(webDriver.getCurrentUrl()).equals(canonizeUrl(signInUrl))) {
            log.debug("Already signed in");
            return;
        }

        log.debug("Signing in");
        wait.untilVisible(cssSelector(".sign-in-wrapper input[name=userId]")).sendKeys(getAuth().getLogin());
        wait.untilVisible(cssSelector(".sign-in-wrapper input[name=inputPassword]")).sendKeys(getAuth().getPassword());
        wait.untilVisible(cssSelector(".sign-in-wrapper #btnSignIn")).click();

        wait.untilUrlIsNot(signInUrl);
    }

    private static void clickClipCouponsButtons(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        webDriver.get("https://safeway.com/foru/coupons-deals.html");

        webDriver.executeScript(
            "document.querySelectorAll('.banner-experiencefragment')"
                + ".forEach(element => element.remove())"
        );

        var containersSelector = cssSelector(".grid-coupon-container");
        var loadMoreSelector = cssSelector(".load-more-container .btn.load-more");
        var loadMore = wait.untilVisible(loadMoreSelector);
        var prevCouponItemsCounter = new AtomicInteger(
            webDriver.findElements(containersSelector).size()
        );
        while (loadMore.isDisplayed()) {
            log.debug("Loading more coupons");
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

        webDriver.executeScript(
            "document.querySelectorAll('.grid-coupon-container .grid-coupon-description-text-details')"
                + ".forEach(element => element.remove())"
        );

        var containers = webDriver.findElements(containersSelector);
        for (var container : containers) {
            final WebElement button;
            try {
                button = container.findElement(cssSelector(".btn.grid-coupon-btn"));
                if (!button.isDisplayed() || !button.isEnabled()) {
                    continue;
                }
            } catch (NotFoundException ignored) {
                continue;
            }

            var title = container.findElement(cssSelector(".grid-coupon-description-text-title"))
                .getText()
                .trim();
            log.info("Clipping Coupon: {}", title);

            button.click();

            await(container).forVisibilityOfElementLocatedBy(cssSelector(".coupon-clipped-container"));
        }
    }


    @Override
    protected String canonizeUrl(String url) {
        var pos = url.indexOf('#');
        if (pos >= 0) {
            url = url.substring(0, pos);
        }

        url = url.replace("://www.", "://");

        return url;
    }

}

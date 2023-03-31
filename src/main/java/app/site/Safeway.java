package app.site;

import static java.util.Collections.reverse;
import static org.openqa.selenium.By.cssSelector;

import app.AbstractSite;
import app.SiteMarker;
import app.utils.ExtendedWebDriverWait;
import java.util.ArrayList;
import java.util.function.IntSupplier;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
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
        log.info("Checking login status");
        var signInUrl = "https://safeway.com/account/sign-in.html";
        webDriver.get(signInUrl);
        wait.randomDuration();

        acceptCookies(webDriver, wait);

        if (!canonizeUrl(webDriver.getCurrentUrl()).equals(canonizeUrl(signInUrl))) {
            log.info("Already signed in");
            return;
        }

        log.info("Signing in");
        wait.untilVisible(cssSelector(".sign-in-wrapper input[name=userId]")).sendKeys(getAuth().getLogin());
        wait.untilVisible(cssSelector(".sign-in-wrapper input[name=inputPassword]")).sendKeys(getAuth().getPassword());
        wait.untilVisible(cssSelector(".sign-in-wrapper #btnSignIn")).click();

        wait.untilUrlIsNot(signInUrl);
    }

    private static void clickClipCouponsButtons(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        log.info("Opening coupons page");
        webDriver.get("https://safeway.com/foru/coupons-deals.html");
        wait.randomDuration();
        acceptCookies(webDriver, wait);


        webDriver.executeScript(
            "document.querySelectorAll('.banner-experiencefragment')"
                + ".forEach(element => element.remove())"
        );


        while (true) {
            clipCoupons(webDriver, wait);

            var areMoreCouponsLoaded = false;
            for (var n = 1; n <= 3; ++n) {
                areMoreCouponsLoaded |= loadMoreCoupons(webDriver, wait);
            }
            if (!areMoreCouponsLoaded) {
                break;
            }
        }
    }

    private static void acceptCookies(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        try {
            var button = webDriver.findElement(cssSelector("#onetrust-accept-btn-handler"));
            if (button.isDisplayed() && button.isEnabled()) {
                button.click();
                wait.randomDuration();
            }
        } catch (NotFoundException ignored) {
            // do nothing
        }
    }

    private static boolean clipCoupons(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        var isAnyCouponClipped = false;
        while (true) {
            var isCouponClipped = false;

            var couponContainers = new ArrayList<>(
                webDriver.findElements(cssSelector(".grid-coupon-container:has(.btn.grid-coupon-btn)"))
            );
            reverse(couponContainers);
            for (var couponContainer : couponContainers) {
                isCouponClipped = clipCoupon(couponContainer, wait);
                if (isCouponClipped) {
                    isAnyCouponClipped = true;
                    break;
                }
            }

            if (!isCouponClipped) {
                return isAnyCouponClipped;
            }
        }
    }

    private static boolean clipCoupon(WebElement couponContainer, ExtendedWebDriverWait wait) {
        final WebElement button;
        try {
            button = couponContainer.findElement(cssSelector(".btn.grid-coupon-btn"));
            if (!button.isDisplayed() || !button.isEnabled()) {
                return false;
            }
        } catch (NotFoundException ignored) {
            return false;
        }

        var title = couponContainer.findElement(cssSelector(".grid-coupon-description-text-title"))
            .getText()
            .trim();

        log.info("Clipping coupon: {}", title);
        button.click();

        wait.until(__ -> {
            try {
                if (!couponContainer.isDisplayed()) {
                    return true;
                }
            } catch (StaleElementReferenceException ignored) {
                return true;
            }

            try {
                var clippedMarker = couponContainer.findElement(cssSelector(".coupon-clipped-container"));
                if (clippedMarker.isDisplayed()) {
                    return true;
                }
            } catch (NotFoundException ignored) {
                // do nothing
            }

            return false;
        });

        return true;
    }

    private static boolean loadMoreCoupons(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        final WebElement button;
        try {
            button = webDriver.findElement(cssSelector(".load-more-container .btn.load-more"));
            if (!button.isDisplayed() || !button.isEnabled()) {
                return false;
            }
        } catch (NotFoundException ignored) {
            return false;
        }

        IntSupplier getCouponsCount = () -> webDriver.findElements(cssSelector(".grid-coupon-container")).size();
        var couponsCountBefore = getCouponsCount.getAsInt();

        log.info("Loading more coupons");
        button.click();

        wait.until(__ -> {
            var couponsCount = getCouponsCount.getAsInt();
            return couponsCount > couponsCountBefore;
        });

        webDriver.executeScript("window.scrollTo(0, -document.body.scrollHeight)");

        return true;
    }


    @Override
    protected String canonizeUrl(String url) {
        url = super.canonizeUrl(url);

        var pos = url.indexOf('#');
        if (pos >= 0) {
            url = url.substring(0, pos);
        }

        return url;
    }

}

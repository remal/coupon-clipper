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

    @SuppressWarnings("java:S3776")
    private static void clickClipCouponsButtons(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        log.info("Loading all coupons");
        webDriver.get("https://safeway.com/foru/coupons-deals.html");
        wait.randomDuration();
        acceptCookies(webDriver, wait);


        webDriver.executeScript(
            "document.querySelectorAll('.banner-experiencefragment')"
                + ".forEach(element => element.remove())"
        );

        var containersSelectorCss = ".grid-coupon-container";
        var containersSelector = cssSelector(containersSelectorCss);
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
        }
        webDriver.executeScript("window.scrollTo(0, -document.body.scrollHeight)");


        log.info("Clipping all coupons");

        webDriver.executeScript(
            "document.querySelectorAll('.grid-coupon-container .grid-coupon-description-text-details')"
                + ".forEach(element => element.remove())"
        );

        while (true) {
            var isAnyButtonClicked = false;
            var containers = webDriver.findElements(containersSelector);
            for (var container : containers) {
                final WebElement button;
                try {
                    button = container.findElement(cssSelector(".btn.grid-coupon-btn"));
                    if (!button.isDisplayed() || !button.isEnabled()) {
                        continue;
                    }
                } catch (NotFoundException | StaleElementReferenceException ignored) {
                    continue;
                }

                var title = container.findElement(cssSelector(".grid-coupon-description-text-title"))
                    .getText()
                    .trim();
                log.info("Clipping Coupon: {}", title);

                button.click();
                wait.randomDuration();

                isAnyButtonClicked = true;
                break;
            }

            if (!isAnyButtonClicked) {
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

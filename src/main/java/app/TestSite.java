package app;

import app.utils.ExtendedWebDriverWait;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.remote.RemoteWebDriver;

@Value
@Jacksonized
@SuperBuilder
@Log4j2
@SiteMarker
public class TestSite extends AbstractSite {

    @Override
    protected void clipCouponsImpl(RemoteWebDriver webDriver, ExtendedWebDriverWait wait) {
        log.info("Loading google.com");
        webDriver.get("https://google.com/");
    }

}

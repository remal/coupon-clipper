package app.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.service.AutoService;
import java.util.Date;
import java.util.Map;
import org.openqa.selenium.Cookie;

@AutoService(Module.class)
public class CookieJacksonModule extends SimpleModule {

    {
        setMixInAnnotation(Cookie.class, CookieMixin.class);
    }


    private abstract static class CookieMixin {

        @JsonCreator
        public CookieMixin(
            @JsonProperty String name,
            @JsonProperty String value,
            @JsonProperty String domain,
            @JsonProperty String path,
            @JsonProperty Date expiry,
            @JsonProperty("secure") boolean secure,
            @JsonProperty("httpOnly") boolean httpOnly,
            @JsonProperty String sameSite
        ) {
        }

        @JsonValue
        public abstract Map<String, Object> toJson();

    }

}

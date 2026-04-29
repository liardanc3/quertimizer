package com.quertimizer.global.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private RememberMe rememberMe = new RememberMe();
    private ClientIp clientIp = new ClientIp();

    @Getter
    @Setter
    public static class RememberMe {
        private String key;
        private Duration validity = Duration.ofDays(14);
        private boolean secure;
        private SameSite sameSite = SameSite.LAX;
    }

    @Getter
    @Setter
    public static class ClientIp {
        private boolean trustForwardedHeaders;
        private List<String> trustedProxies = List.of();
    }
}

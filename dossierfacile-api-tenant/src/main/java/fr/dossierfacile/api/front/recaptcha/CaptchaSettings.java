package fr.dossierfacile.api.front.recaptcha;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "google.recaptcha.key")
@Getter
@Setter
public class CaptchaSettings {
    private String site;
    private String secret;
    private String url;
    private boolean disabled;
}
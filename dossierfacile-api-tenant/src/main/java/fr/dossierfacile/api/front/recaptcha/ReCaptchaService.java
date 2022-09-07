package fr.dossierfacile.api.front.recaptcha;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReCaptchaService {

    private final RestOperations restTemplate;

    private final CaptchaSettings captchaSettings;

    private final HttpServletRequest request;

    @Value("${spring.profiles.active}")
    private String activeProfiles;

    boolean validate(String reCaptchaResponse) {
        if (Arrays.asList(activeProfiles.split(",")).stream().anyMatch(profile -> "dev".equals(profile) || "preprod".equals(profile))) {
            return true;
        }
        URI verifyUri = URI.create(String.format(
                "%s?secret=%s&response=%s&remoteip=%s",
                captchaSettings.getUrl(),
                captchaSettings.getSecret(),
                reCaptchaResponse,
                request.getRemoteAddr()
        ));

        try {
            ReCaptchaResponse response = restTemplate.getForObject(verifyUri, ReCaptchaResponse.class);
            return Objects.requireNonNull(response).isSuccess();
        } catch (Exception e) {
            log.error("Problem when trying to get the Captcha code", e);
            return false;
        }
    }

}

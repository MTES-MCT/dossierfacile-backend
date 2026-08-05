package fr.dossierfacile.common.validator;

import fr.dossierfacile.common.validator.annotation.ValidUrl;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.URI;

@Slf4j
public class ValidUrlValidator implements ConstraintValidator<ValidUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true;
        }

        return isValidUrl(value);
    }

    public static boolean isValidUrl(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }

            String host = uri.getHost();
            if (StringUtils.isBlank(host)) {
                return false;
            }

            if (isForbiddenHost(host)) {
                log.warn("Blocked potential SSRF attempt to forbidden host: {}", host);
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isForbiddenHost(String host) {
        if (host == null || host.isBlank()) {
            return true;
        }
        String cleanHost = host.trim().toLowerCase();
        if (cleanHost.equals("localhost") || cleanHost.endsWith(".localhost") || cleanHost.equals("0.0.0.0")) {
            return true;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(cleanHost);
            for (InetAddress address : addresses) {
                if (address.isLoopbackAddress()
                        || address.isSiteLocalAddress()
                        || address.isLinkLocalAddress()
                        || address.isAnyLocalAddress()
                        || address.isMulticastAddress()) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Unable to resolve host: {}", cleanHost);
        }
        return false;
    }
}

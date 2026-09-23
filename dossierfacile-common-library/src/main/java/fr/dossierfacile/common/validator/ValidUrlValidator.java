package fr.dossierfacile.common.validator;

import fr.dossierfacile.common.validator.annotation.ValidUrl;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

@Slf4j
public class ValidUrlValidator implements ConstraintValidator<ValidUrl, String> {

    /**
     * Resolves a host name to its addresses. Extracted so the resolution can be stubbed in tests.
     */
    @FunctionalInterface
    public interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    /**
     * Hosts under these domains are DossierFacile's own services or apps hosted on the same PaaS.
     * Seen from inside the platform, they resolve to internal load-balancer addresses, so the
     * private-address check below would wrongly reject them. Matching is on the domain suffix: {@code dossierfacile.fr} and any subdomain.
     */
    static final List<String> TRUSTED_DOMAINS = List.of("dossierfacile.fr");

    private static final HostResolver DNS_RESOLVER = InetAddress::getAllByName;

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
        return isForbiddenHost(host, DNS_RESOLVER);
    }

    static boolean isForbiddenHost(String host, HostResolver resolver) {
        if (host == null || host.isBlank()) {
            return true;
        }
        String cleanHost = host.trim().toLowerCase();
        if (cleanHost.equals("localhost") || cleanHost.endsWith(".localhost") || cleanHost.equals("0.0.0.0")) {
            return true;
        }
        // A trusted domain is never an internal service, whatever it resolves to from here
        if (isTrustedHost(cleanHost)) {
            return false;
        }

        try {
            InetAddress[] addresses = resolver.resolve(cleanHost);
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

    static boolean isTrustedHost(String cleanHost) {
        return TRUSTED_DOMAINS.stream()
                .anyMatch(domain -> cleanHost.equals(domain) || cleanHost.endsWith("." + domain));
    }
}

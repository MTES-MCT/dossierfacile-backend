package fr.dossierfacile.common.validator;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class ValidUrlValidatorTest {

    private final ValidUrlValidator validator = new ValidUrlValidator();

    /**
     * Mimics the platform-internal DNS: every host resolves to a private load-balancer address
     */
    private static final ValidUrlValidator.HostResolver PRIVATE_RESOLVER =
            host -> new InetAddress[]{InetAddress.getByName("10.0.0.1")};

    @Test
    void isValid_shouldRejectLoopbackAndLocalhost() {
        assertThat(validator.isValid("http://127.0.0.1", null)).isFalse();
        assertThat(validator.isValid("http://127.0.0.1:8080/callback", null)).isFalse();
        assertThat(validator.isValid("http://localhost/api", null)).isFalse();
        assertThat(validator.isValid("https://localhost:8443/webhook", null)).isFalse();
        assertThat(validator.isValid("http://0.0.0.0", null)).isFalse();
    }

    @Test
    void isValid_shouldRejectPrivateNetworkIPs() {
        // RFC 1918 private IPs
        assertThat(validator.isValid("http://10.0.0.1/webhook", null)).isFalse();
        assertThat(validator.isValid("http://192.168.1.50/callback", null)).isFalse();
        assertThat(validator.isValid("http://172.16.0.1/callback", null)).isFalse();
        // Cloud metadata IP
        assertThat(validator.isValid("http://169.254.169.254/latest/meta-data/", null)).isFalse();
    }

    @Test
    void isValid_shouldRejectNonHttpProtocols() {
        assertThat(validator.isValid("file:///etc/passwd", null)).isFalse();
        assertThat(validator.isValid("gopher://127.0.0.1:70/", null)).isFalse();
        assertThat(validator.isValid("ftp://example.com/file", null)).isFalse();
    }

    @Test
    void isValid_shouldAcceptValidPublicHttpsUrl() {
        assertThat(validator.isValid("https://partner.example.com/webhook", null)).isTrue();
        assertThat(validator.isValid("http://partner-service.fr/callback", null)).isTrue();
    }

    @Test
    void isForbiddenHost_shouldAcceptTrustedDomainsEvenWhenTheyResolveToPrivateAddresses() {
        assertThat(ValidUrlValidator.isForbiddenHost("demo-partenaire.dossierfacile.fr", PRIVATE_RESOLVER)).isFalse();
        assertThat(ValidUrlValidator.isForbiddenHost("DossierFacile.fr", PRIVATE_RESOLVER)).isFalse();
    }

    @Test
    void isForbiddenHost_shouldStillRejectUntrustedHostsResolvingToPrivateAddresses() {
        assertThat(ValidUrlValidator.isForbiddenHost("partner.example.com", PRIVATE_RESOLVER)).isTrue();
        // Look-alike domains are not trusted: suffix match is on a full label boundary
        assertThat(ValidUrlValidator.isForbiddenHost("evildossierfacile.fr", PRIVATE_RESOLVER)).isTrue();
        assertThat(ValidUrlValidator.isForbiddenHost("dossierfacile.fr.evil.com", PRIVATE_RESOLVER)).isTrue();
    }

    @Test
    void isForbiddenHost_shouldRejectLocalhostBeforeTrustCheck() {
        assertThat(ValidUrlValidator.isForbiddenHost("localhost", PRIVATE_RESOLVER)).isTrue();
        assertThat(ValidUrlValidator.isForbiddenHost("api.localhost", PRIVATE_RESOLVER)).isTrue();
    }

    @Test
    void isForbiddenHost_shouldAcceptUnresolvableHost() {
        ValidUrlValidator.HostResolver failingResolver = host -> {
            throw new UnknownHostException(host);
        };
        assertThat(ValidUrlValidator.isForbiddenHost("unknown.example.com", failingResolver)).isFalse();
    }
}

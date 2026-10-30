package fr.dossierfacile.dfcdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings a partner needs to integrate DossierFacile Connect. They mirror the environment variables of the
 * original PHP demo.
 *
 * @param serverBaseUrl public base URL of this demo, used to build the OIDC redirect URI
 * @param ssoBaseUrl    root URL of the DossierFacile Keycloak, without the {@code /auth} path
 * @param apiBaseUrl    root URL of the DossierFacile tenant API
 * @param clientId      Keycloak client id of the partner (must match {@code user_api.name} on the DossierFacile side)
 * @param clientSecret  Keycloak client secret of the partner, never rendered nor logged
 * @param webhookApiKey value DossierFacile sends in the {@code x-api-key} header of each webhook
 */
@ConfigurationProperties("dfc-demo")
public record DfcDemoProperties(
        String serverBaseUrl,
        String ssoBaseUrl,
        String apiBaseUrl,
        String clientId,
        String clientSecret,
        String webhookApiKey) {
}

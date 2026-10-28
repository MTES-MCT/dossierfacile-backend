package fr.dossierfacile.dfcdemo.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.dfcdemo.config.DfcDemoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * The DossierFacile Connect flow, written as explicit HTTP calls so that partners can transpose it to any stack:
 * authorization code request, token exchange, profile retrieval and logout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DfcConnectService {

    public static final String STEP_AUTHORIZE = "authorize";
    public static final String STEP_TOKEN = "token";
    public static final String STEP_PROFILE = "profile";
    public static final String STEP_LOGOUT = "logout";

    private static final String OIDC_PATH = "/auth/realms/dossier-facile/protocol/openid-connect";
    private static final String PROFILE_PATH = "/dfc/tenant/profile";
    private static final String LOGOUT_PATH = "/dfc/tenant/logout";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DfcDemoProperties properties;

    /** Where Keycloak sends the user back with the authorization code. Must be registered on the Keycloak client. */
    public String redirectUri() {
        return properties.serverBaseUrl() + "/display";
    }

    public String tokenUrl() {
        return properties.ssoBaseUrl() + OIDC_PATH + "/token";
    }

    public String profileUrl() {
        return properties.apiBaseUrl() + PROFILE_PATH;
    }

    public String logoutUrl() {
        return properties.apiBaseUrl() + LOGOUT_PATH;
    }

    /** Step 0: the URL the partner site redirects the tenant to. */
    public String authorizeUrl() {
        // Template variables are strictly encoded (":" and "/" included), unlike literal query values
        return UriComponentsBuilder.fromUriString(properties.ssoBaseUrl() + OIDC_PATH + "/auth")
                .queryParam("scope", "openid")
                .queryParam("access_type", "online")
                .queryParam("redirect_uri", "{redirectUri}")
                .queryParam("response_type", "code")
                .queryParam("client_id", "{clientId}")
                .encode()
                .buildAndExpand(redirectUri(), properties.clientId())
                .toUriString();
    }

    /** Step 2: exchange the authorization code for an access token (server to server, carries the client secret). */
    public String exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", redirectUri());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = call(STEP_TOKEN,
                () -> restTemplate.postForEntity(tokenUrl(), new HttpEntity<>(form, headers), String.class));
        String accessToken = readJson(STEP_TOKEN, body).path("access_token").asText();
        if (accessToken.isBlank()) {
            throw new DfcApiException(STEP_TOKEN, null, body);
        }
        return accessToken;
    }

    /** Step 3: the mandatory profile call, which links the tenant to the partner on the DossierFacile side. */
    public String fetchProfile(String accessToken) {
        String body = call(STEP_PROFILE,
                () -> restTemplate.exchange(profileUrl(), HttpMethod.GET, bearer(accessToken), String.class));
        return prettyPrint(body);
    }

    /** Ends the tenant's Keycloak sessions, useful when the tenant linked the wrong DossierFacile account. */
    public void logout(String accessToken) {
        call(STEP_LOGOUT,
                () -> restTemplate.exchange(logoutUrl(), HttpMethod.GET, bearer(accessToken), String.class));
    }

    private static HttpEntity<Void> bearer(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    private static String call(String step, Supplier<ResponseEntity<String>> request) {
        try {
            return request.get().getBody();
        } catch (RestClientResponseException e) {
            throw new DfcApiException(step, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.warn("DFC step {} unreachable: {}", step, e.getMessage());
            throw new DfcApiException(step, null, e.getMessage());
        }
    }

    private JsonNode readJson(String step, String body) {
        try {
            return objectMapper.readTree(body == null ? "" : body);
        } catch (IOException e) {
            throw new DfcApiException(step, null, body);
        }
    }

    private String prettyPrint(String body) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(body));
        } catch (IOException | NullPointerException e) {
            return body;
        }
    }
}

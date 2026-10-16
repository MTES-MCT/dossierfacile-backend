package fr.dossierfacile.api.front.das.mesri;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
public class FranceConnectTokenServiceImpl implements FranceConnectTokenService {

    private static final String BROKER_ALIAS = "oidc";

    @Value("${keycloak.server.url}")
    private String keycloakServerUrl;
    @Value("${keycloak.server.realm}")
    private String keycloakRealm;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    FranceConnectTokenServiceImpl(String keycloakServerUrl, String keycloakRealm, HttpClient httpClient,
            ObjectMapper objectMapper, Clock clock) {
        this.keycloakServerUrl = keycloakServerUrl;
        this.keycloakRealm = keycloakRealm;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Autowired
    FranceConnectTokenServiceImpl(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Override
    public FranceConnectAccessToken getFranceConnectAccessToken(String userJwt) {
        URI uri = URI.create(keycloakServerUrl + "/realms/" + keycloakRealm + "/broker/" + BROKER_ALIAS + "/token");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + userJwt)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new FranceConnectTokenUnavailableException("Keycloak broker endpoint unreachable: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FranceConnectTokenUnavailableException("Interrupted while calling Keycloak broker endpoint");
        }

        if (response.statusCode() != 200) {
            log.warn("Keycloak broker token endpoint answered {}: {}", response.statusCode(),
                    StringUtils.abbreviate(response.body(), 200));
            throw new FranceConnectTokenUnavailableException(
                    "Keycloak did not return the FranceConnect token (HTTP " + response.statusCode() + ")");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new FranceConnectTokenUnavailableException("Unreadable broker token response");
        }
        String accessToken = root.path("access_token").asText(null);
        if (StringUtils.isBlank(accessToken)) {
            throw new FranceConnectTokenUnavailableException("No FranceConnect access token stored for this user");
        }
        Long expiresAt = root.hasNonNull("accessTokenExpiration") ? root.get("accessTokenExpiration").asLong() : null;
        FranceConnectAccessToken token = new FranceConnectAccessToken(accessToken, root.path("scope").asText(null), expiresAt);

        Instant now = Instant.now(clock);
        if (token.isExpired(now)) {
            throw new FranceConnectTokenUnavailableException("FranceConnect token expired, a new FranceConnect login is required");
        }
        log.info("FranceConnect token read from Keycloak, {} s left, scopes: {}", token.secondsLeft(now), token.scope());
        return token;
    }
}

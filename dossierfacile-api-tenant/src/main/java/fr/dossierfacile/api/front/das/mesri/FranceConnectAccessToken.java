package fr.dossierfacile.api.front.das.mesri;

import java.time.Instant;

/**
 * FranceConnect access token stored by Keycloak after a FranceConnect login and read back
 * through the broker endpoint. It lives about 60 seconds and is never refreshed.
 *
 * @param accessToken        the raw FranceConnect access token
 * @param scope              the scopes actually granted by FranceConnect (space separated), may be null
 * @param expiresAtEpochSecs expiration as reported by Keycloak ({@code accessTokenExpiration}), may be null
 */
public record FranceConnectAccessToken(String accessToken, String scope, Long expiresAtEpochSecs) {

    public boolean isExpired(Instant now) {
        return expiresAtEpochSecs != null && expiresAtEpochSecs <= now.getEpochSecond();
    }

    public long secondsLeft(Instant now) {
        return expiresAtEpochSecs == null ? -1 : expiresAtEpochSecs - now.getEpochSecond();
    }
}

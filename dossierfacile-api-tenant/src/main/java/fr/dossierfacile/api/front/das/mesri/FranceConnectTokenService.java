package fr.dossierfacile.api.front.das.mesri;

public interface FranceConnectTokenService {

    /**
     * Reads the FranceConnect access token that Keycloak stored for the logged user
     * ({@code GET /realms/{realm}/broker/oidc/token}, authenticated with the user's own JWT).
     *
     * @throws FranceConnectTokenUnavailableException when Keycloak refuses (missing broker/read-token role),
     *                                                has no token for this user, or the token is already expired
     */
    FranceConnectAccessToken getFranceConnectAccessToken(String userJwt);
}

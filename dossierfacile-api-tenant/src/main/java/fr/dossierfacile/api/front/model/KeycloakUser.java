package fr.dossierfacile.api.front.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KeycloakUser {

    String keycloakId;
    String email;
    boolean emailVerified;
    String preferredUsername;
    String givenName;
    String familyName;
    boolean franceConnect;

}

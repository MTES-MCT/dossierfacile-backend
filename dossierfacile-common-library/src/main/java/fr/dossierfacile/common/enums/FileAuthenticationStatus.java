package fr.dossierfacile.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileAuthenticationStatus {

    VALID("Authentifié"),
    INVALID("Non authentifié"),
    API_ERROR("Erreur lors de l'authentification"),
    UNKNOWN_DOCUMENT("Non reconnu ou expiré")
    ;

    private final String label;

    public static FileAuthenticationStatus of(boolean isValid) {
        return isValid ? VALID : INVALID;
    }

}

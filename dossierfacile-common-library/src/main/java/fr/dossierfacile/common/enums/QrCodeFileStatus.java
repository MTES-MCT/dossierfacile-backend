package fr.dossierfacile.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QrCodeFileStatus {

    VALID("Authentifié"),
    INVALID("Non authentifié"),
    API_ERROR("Erreur lors de l'authentification"),
    UNKNOWN_DOCUMENT("Non reconnu ou expiré"),
    WRONG_CATEGORY("Ne correspond pas à la catégorie")
    ;

    private final String label;

    public static QrCodeFileStatus of(boolean isValid) {
        return isValid ? VALID : INVALID;
    }

}

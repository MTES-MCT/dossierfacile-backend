package fr.dossierfacile.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MonFranceConnectValidationStatus {

    VALID("Authentifié"),
    INVALID("Non authentifié"),
    API_ERROR("Erreur lors de l'appel à MonFranceConnect"),
    UNKNOWN_DOCUMENT("Non reconnu par MonFranceConnect (peut-être expiré)"),
    WRONG_CATEGORY("Ne correspond pas à la catégorie")
    ;

    private final String label;

    public static MonFranceConnectValidationStatus of(boolean isValid) {
        return isValid ? VALID : INVALID;
    }

}

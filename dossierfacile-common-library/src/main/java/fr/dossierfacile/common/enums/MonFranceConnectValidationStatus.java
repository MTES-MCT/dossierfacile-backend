package fr.dossierfacile.common.enums;

public enum MonFranceConnectValidationStatus {

    VALID,
    INVALID,
    API_ERROR,
    UNKNOWN_DOCUMENT;

    public static MonFranceConnectValidationStatus of(boolean isValid) {
        return isValid ? VALID : INVALID;
    }

}

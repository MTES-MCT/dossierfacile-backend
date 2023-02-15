package fr.dossierfacile.process.file.service.monfranceconnect.validation;

public enum ValidationStatus {

    VALID,
    INVALID,
    API_ERROR;

    static ValidationStatus of(boolean isValid) {
        return isValid ? VALID : INVALID;
    }

}

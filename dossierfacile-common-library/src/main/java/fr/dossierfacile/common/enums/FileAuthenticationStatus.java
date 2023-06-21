package fr.dossierfacile.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileAuthenticationStatus {

    VALID,
    INVALID,
    API_ERROR,
    ERROR,
    UNKNOWN_DOCUMENT,
    ;

    public static FileAuthenticationStatus of(boolean isAuthentic) {
        return isAuthentic ? VALID : INVALID;
    }

}

package fr.gouv.owner.security;


public enum ChangePasswordStatus {
    SUCCESS,
    WRONG_PASSWORD,
    TOKEN_TOO_OLD,
    TOKEN_DOES_NOT_EXIST
}

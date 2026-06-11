package fr.gouv.bo.security;

import org.springframework.security.access.AccessDeniedException;

public final class BOAccessDenied {

    public static final String GENERIC_MESSAGE = "Accès refusé";

    private BOAccessDenied() {
    }

    public static AccessDeniedException generic() {
        return new AccessDeniedException(GENERIC_MESSAGE);
    }
}

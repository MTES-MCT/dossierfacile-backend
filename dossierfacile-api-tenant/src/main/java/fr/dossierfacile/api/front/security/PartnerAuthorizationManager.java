package fr.dossierfacile.api.front.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.function.Supplier;

public class PartnerAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private final String authScope;

    public PartnerAuthorizationManager(String scope) {
        this.authScope = "SCOPE_" + scope;
    }

    private boolean hasScope(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> authScope.equals(a.getAuthority()));
    }

    private boolean isClient(Authentication authentication) {
        try {
            return ((Jwt) authentication.getPrincipal()).getClaimAsString("client_id") != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        return new AuthorizationDecision(isClient(authentication.get()) && hasScope(authentication.get()));
    }
}
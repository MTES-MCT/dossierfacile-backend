package fr.dossierfacile.api.front.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static fr.dossierfacile.authentification.JwtFactoryKt.getDummyJwtWithCustomClaims;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PartnerAuthorizationManagerTest {

    private PartnerAuthorizationManager partnerAuthorizationManager;
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        partnerAuthorizationManager = new PartnerAuthorizationManager("dfc");
        httpServletRequest = mock(HttpServletRequest.class);
    }

    @Test
    void shouldAuthorizeWhenClientIdPresentAndCorrectScope() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_id", "partner-client");
        Jwt jwt = getDummyJwtWithCustomClaims(claims);
        Authentication authentication = new JwtAuthenticationToken(jwt, Collections.singletonList(new SimpleGrantedAuthority("SCOPE_dfc")));

        Supplier<Authentication> authSupplier = () -> authentication;
        RequestAuthorizationContext context = new RequestAuthorizationContext(httpServletRequest, Map.of());

        AuthorizationDecision decision = partnerAuthorizationManager.check(authSupplier, context);

        assertThat(decision).isNotNull();
        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    void shouldDenyWhenClientIdMissing() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-123");
        Jwt jwt = getDummyJwtWithCustomClaims(claims);
        Authentication authentication = new JwtAuthenticationToken(jwt, Collections.singletonList(new SimpleGrantedAuthority("SCOPE_dfc")));

        Supplier<Authentication> authSupplier = () -> authentication;
        RequestAuthorizationContext context = new RequestAuthorizationContext(httpServletRequest, Map.of());

        AuthorizationDecision decision = partnerAuthorizationManager.check(authSupplier, context);

        assertThat(decision).isNotNull();
        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void shouldDenyWhenWrongScope() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_id", "partner-client");
        Jwt jwt = getDummyJwtWithCustomClaims(claims);
        Authentication authentication = new JwtAuthenticationToken(jwt, Collections.singletonList(new SimpleGrantedAuthority("SCOPE_dossier")));

        Supplier<Authentication> authSupplier = () -> authentication;
        RequestAuthorizationContext context = new RequestAuthorizationContext(httpServletRequest, Map.of());

        AuthorizationDecision decision = partnerAuthorizationManager.check(authSupplier, context);

        assertThat(decision).isNotNull();
        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void shouldDenyWhenNoScope() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client_id", "partner-client");
        Jwt jwt = getDummyJwtWithCustomClaims(claims);
        Authentication authentication = new JwtAuthenticationToken(jwt, Collections.emptyList());

        Supplier<Authentication> authSupplier = () -> authentication;
        RequestAuthorizationContext context = new RequestAuthorizationContext(httpServletRequest, Map.of());

        AuthorizationDecision decision = partnerAuthorizationManager.check(authSupplier, context);

        assertThat(decision).isNotNull();
        assertThat(decision.isGranted()).isFalse();
    }
}

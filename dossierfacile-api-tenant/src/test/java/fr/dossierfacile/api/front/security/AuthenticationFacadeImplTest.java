package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

import static fr.dossierfacile.authentification.JwtFactoryKt.getDummyJwt;
import static fr.dossierfacile.authentification.JwtFactoryKt.getDummyJwtWithCustomClaims;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"keycloak.server.url= http://localhost:8085/auth", "keycloak.server.realm= test"})
class AuthenticationFacadeImplTest {

    private static final TenantCommonRepository tenantCommonRepository = mock(TenantCommonRepository.class);
    private static final TenantPermissionsService tenantPermissionsService = mock(TenantPermissionsService.class);
    private static final TenantStatusService tenantStatusService = mock(TenantStatusService.class);
    private static final TenantService tenantService = mock(TenantService.class);
    private static final LogService logService = mock(LogService.class);
    private static final DocumentService documentService = mock(DocumentService.class);
    private static final fr.dossierfacile.api.front.domain.service.FindOrCreateTenantDomainService findOrCreateTenantDomainService =
            new fr.dossierfacile.api.front.domain.service.FindOrCreateTenantDomainService(tenantCommonRepository, tenantService);

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @TestConfiguration
    static class AuthentificationFacadeImplTestContextConfiguration {

        @Bean
        public AuthenticationFacade getAuthenticationFacade() {
            return new AuthenticationFacadeImpl(
                    tenantCommonRepository,
                    tenantPermissionsService,
                    findOrCreateTenantDomainService
            );
        }
    }

    @BeforeEach
    void before() {
        reset(
                tenantCommonRepository,
                tenantPermissionsService,
                tenantStatusService,
                tenantService,
                logService,
                documentService
        );
    }

    @Nested
    class GetKeycloakClientIdTest {

        @Test
        void shouldReturnNullWhenAzpIsNotDefined() {
            var jwt = getDummyJwt();
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));
            var result = authenticationFacade.getKeycloakClientId();
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnAzpFromJwt() {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("azp", "azp");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);

            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));
            var result = authenticationFacade.getKeycloakClientId();
            assertThat(result).isEqualTo("azp");
        }
    }

    @Nested
    class GetUserEmailTest {

        @Test
        void shouldReturnNullWhenEmailIsNotDefined() {
            var jwt = getDummyJwt();
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));
            var result = authenticationFacade.getUserEmail();
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnEmailFromJwt() {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("email", "email");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);

            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));
            var result = authenticationFacade.getUserEmail();
            assertThat(result).isEqualTo("email");
        }
    }

    @Nested
    class GetKeycloakUserIdTest {

        @Test
        void shouldReturnNullWhenSubIsNotDefined() {
            var jwt = getDummyJwt();
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));
            var result = authenticationFacade.getKeycloakUserId();
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnSubFromJwt() {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("sub", "sub");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);

            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));
            var result = authenticationFacade.getKeycloakUserId();
            assertThat(result).isEqualTo("sub");
        }
    }

    @Nested
    class GetKeycloakUserTest {

        @Test
        void shouldThrowNullPointerWhenEmailVerifiedClaimUndefined() {
            var jwt = getDummyJwt();
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));

            assertThrows(NullPointerException.class, () -> authenticationFacade.getKeycloakUser());
        }

        @Test
        void shouldReturnKeycloakUserWithoutPreferredName() {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("sub", "sub");
            claimsMap.put("email", "test@test.fr");
            claimsMap.put("given_name", "test");
            claimsMap.put("family_name", "test");
            claimsMap.put("email_verified", true);
            claimsMap.put("france-connect", true);
            claimsMap.put("france-connect-sub", "fsub");
            claimsMap.put("birthcountry", "test");
            claimsMap.put("birthplace", "test");
            claimsMap.put("birthdate", "test");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));

            var result = authenticationFacade.getKeycloakUser();
            assertThat(result.getKeycloakId()).isEqualTo("sub");
            assertThat(result.getEmail()).isEqualTo("test@test.fr");
            assertThat(result.getGivenName()).isEqualTo("test");
            assertThat(result.getFamilyName()).isEqualTo("test");
            assertThat(result.isEmailVerified()).isTrue();
            assertThat(result.isFranceConnect()).isTrue();
            assertThat(result.getPreferredUsername()).isNull();
            assertThat(result.getComputedPreferredUserName()).isNull();
            assertThat(result.getFranceConnectSub()).isEqualTo("fsub");
            assertThat(result.getFranceConnectBirthCountry()).isEqualTo("test");
            assertThat(result.getFranceConnectBirthPlace()).isEqualTo("test");
            assertThat(result.getFranceConnectBirthDate()).isEqualTo("test");
        }

        @Test
        void shouldReturnKeycloakUserWithoutPreferredNameWhenIncorrectChar() {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("sub", "sub");
            claimsMap.put("email", "test@test.fr");
            claimsMap.put("given_name", "test");
            claimsMap.put("family_name", "test");
            claimsMap.put("email_verified", true);
            claimsMap.put("france-connect", true);
            claimsMap.put("preferred_username", "test@test.fr");
            claimsMap.put("france-connect-sub", "fsub");
            claimsMap.put("birthcountry", "test");
            claimsMap.put("birthplace", "test");
            claimsMap.put("birthdate", "test");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));

            var result = authenticationFacade.getKeycloakUser();
            assertThat(result.getKeycloakId()).isEqualTo("sub");
            assertThat(result.getEmail()).isEqualTo("test@test.fr");
            assertThat(result.getGivenName()).isEqualTo("test");
            assertThat(result.getFamilyName()).isEqualTo("test");
            assertThat(result.isEmailVerified()).isTrue();
            assertThat(result.isFranceConnect()).isTrue();
            assertThat(result.getPreferredUsername()).isEqualTo("test@test.fr");
            assertThat(result.getComputedPreferredUserName()).isNull();
            assertThat(result.getFranceConnectSub()).isEqualTo("fsub");
            assertThat(result.getFranceConnectBirthCountry()).isEqualTo("test");
            assertThat(result.getFranceConnectBirthPlace()).isEqualTo("test");
            assertThat(result.getFranceConnectBirthDate()).isEqualTo("test");
        }

        @Test
        void shouldReturnKeycloakUserWithPreferredName() {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("sub", "sub");
            claimsMap.put("email", "test@test.fr");
            claimsMap.put("given_name", "test");
            claimsMap.put("family_name", "test");
            claimsMap.put("email_verified", true);
            claimsMap.put("france-connect", true);
            claimsMap.put("preferred_username", "test");
            claimsMap.put("france-connect-sub", "fsub");
            claimsMap.put("birthcountry", "test");
            claimsMap.put("birthplace", "test");
            claimsMap.put("birthdate", "test");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));

            var result = authenticationFacade.getKeycloakUser();
            assertThat(result.getKeycloakId()).isEqualTo("sub");
            assertThat(result.getEmail()).isEqualTo("test@test.fr");
            assertThat(result.getGivenName()).isEqualTo("test");
            assertThat(result.getFamilyName()).isEqualTo("test");
            assertThat(result.isEmailVerified()).isTrue();
            assertThat(result.isFranceConnect()).isTrue();
            assertThat(result.getPreferredUsername()).isEqualTo("test");
            assertThat(result.getComputedPreferredUserName()).isEqualTo("test");
            assertThat(result.getFranceConnectSub()).isEqualTo("fsub");
            assertThat(result.getFranceConnectBirthCountry()).isEqualTo("test");
            assertThat(result.getFranceConnectBirthPlace()).isEqualTo("test");
            assertThat(result.getFranceConnectBirthDate()).isEqualTo("test");
        }
    }

    @Nested
    class GetTenantTest {

        @Test
        void shouldReturnAccessDeniedExceptionWhenNoAuthorities() {
            var jwt = getDummyJwt();
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, Collections.emptyList())));
            assertThrows(AccessDeniedException.class, () -> {
                authenticationFacade.getTenant(1L);
            });
        }

        @Test
        void shouldReturnAccessDeniedExceptionWhenWrongAuthority() {
            var jwt = getDummyJwt();
            List<GrantedAuthority> authorities = Collections.singletonList((GrantedAuthority) () -> "ROLE_USER");
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, authorities)));
            assertThrows(AccessDeniedException.class, () -> {
                authenticationFacade.getTenant(1L);
            });
        }

        @Test
        void shouldReturnAccessDeniedWhenTenantCantAccess() {
            var jwt = getDummyJwtWithCustomClaims(Map.of("sub", "keycloakId"));
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_dossier"));
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, authorities)));
            when(tenantPermissionsService.canAccess("keycloakId", 1L)).thenReturn(false);
            assertThrows(AccessDeniedException.class, () -> {
                authenticationFacade.getTenant(1L);
            });
            verify(tenantPermissionsService, times(1)).canAccess("keycloakId", 1L);
        }

        @Test
        void shouldReturnTenantNotFound() {
            var jwt = getDummyJwtWithCustomClaims(Map.of("sub", "keycloakId"));
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_dossier"));
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, authorities)));
            when(tenantPermissionsService.canAccess("keycloakId", 1L)).thenReturn(true);
            when(tenantCommonRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(TenantNotFoundException.class, () -> {
                authenticationFacade.getTenant(1L);
            });
            verify(tenantPermissionsService, times(1)).canAccess("keycloakId", 1L);
            verify(tenantCommonRepository, times(1)).findById(1L);
        }

        @Test
        void shouldReturnTenant() {
            var tenant = Tenant.builder().id(1L).build();
            var jwt = getDummyJwtWithCustomClaims(Map.of("sub", "keycloakId"));
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_dossier"));
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, authorities)));
            when(tenantPermissionsService.canAccess("keycloakId", 1L)).thenReturn(true);
            when(tenantCommonRepository.findById(1L)).thenReturn(Optional.of(tenant));
            var result = authenticationFacade.getTenant(1L);
            verify(tenantPermissionsService, times(1)).canAccess("keycloakId", 1L);
            verify(tenantCommonRepository, times(1)).findById(1L);
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        void shouldReturnAccessDeniedExceptionWhenEmailIsNotVerifiedAndNotFranceConnectWithoutTenantId() {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("sub", "keycloakId");
            claimsMap.put("email", "test@test.fr");
            claimsMap.put("email_verified", false);
            claimsMap.put("france-connect", false);
            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_dossier"));
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, authorities)));
            assertThrows(AccessDeniedException.class, () -> {
                authenticationFacade.getTenant(null);
            });
        }
    }
}
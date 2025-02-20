package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.converter.AcquisitionData;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.mockito.stubbing.Answer;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static fr.dossierfacile.authentification.JwtFactoryKt.getDummyJwt;
import static fr.dossierfacile.authentification.JwtFactoryKt.getDummyJwtWithCustomClaims;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"keycloak.server.url= http://localhost:8085/auth", "keycloak.franceconnect.provider= oidc", "keycloak.server.realm= test"})
public class AuthentificationFacadeImplTest {

    private final static TenantCommonRepository tenantCommonRepository = mock(TenantCommonRepository.class);
    private final static TenantPermissionsService tenantPermissionsService = mock(TenantPermissionsService.class);
    private final static TenantStatusService tenantStatusService = mock(TenantStatusService.class);
    private final static TenantService tenantService = mock(TenantService.class);
    private final static LogService logService = mock(LogService.class);
    private final static DocumentService documentService = mock(DocumentService.class);

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @TestConfiguration
    static class AuthentificationFacadeImplTestContextConfiguration {

        @Bean
        public AuthenticationFacade getAuthenticationFacade() {
            return new AuthenticationFacadeImpl(
                    tenantCommonRepository,
                    tenantPermissionsService,
                    tenantStatusService,
                    tenantService,
                    logService,
                    documentService
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
            assertThat(result.getPreferredUsername()).isNull();
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

    /*
     * We use introspection to test private method to simplify the test
     */
    @Nested
    class FindOrCreateTenantTest {

        @Test
        void shouldCreateAccountFromKeycloakUser() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("sub", "keycloakId");
            claimsMap.put("email", "test@test.fr");
            claimsMap.put("email_verified", false);
            claimsMap.put("france-connect", false);

            var keycloakUser = KeycloakUser.builder()
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .emailVerified(true)
                    .build();

            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_dossier"));
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, authorities)));

            when(tenantCommonRepository.findByKeycloakId("keycloakId")).thenReturn(null);
            when(tenantCommonRepository.findByEmail("test@test.fr")).thenReturn(Optional.empty());
            when(tenantService.registerFromKeycloakUser(keycloakUser, null, null)).thenReturn(Tenant.builder().id(1L).build());

            var methodToTest = authenticationFacade.getClass().getDeclaredMethod("findOrCreateTenant", KeycloakUser.class, AcquisitionData.class);
            methodToTest.setAccessible(true);
            methodToTest.invoke(authenticationFacade, keycloakUser, null);

            verify(tenantCommonRepository, times(1)).findByKeycloakId("keycloakId");
            verify(tenantCommonRepository, times(1)).findByEmail("test@test.fr");
            verify(tenantService, times(1)).registerFromKeycloakUser(keycloakUser, null, null);

        }

        @Test
        void shouldReturnTenantByKeycloakId() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("sub", "keycloakId");
            claimsMap.put("email", "test@test.fr");
            claimsMap.put("email_verified", false);
            claimsMap.put("france-connect", false);

            var keycloakUser = KeycloakUser.builder()
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .emailVerified(true)
                    .build();

            var tenant = Tenant.builder().id(1L).build();

            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_dossier"));
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, authorities)));

            when(tenantCommonRepository.findByKeycloakId("keycloakId")).thenReturn(tenant);

            var methodToTest = authenticationFacade.getClass().getDeclaredMethod("findOrCreateTenant", KeycloakUser.class, AcquisitionData.class);
            methodToTest.setAccessible(true);
            var result = methodToTest.invoke(authenticationFacade, keycloakUser, null);

            verify(tenantCommonRepository, times(1)).findByKeycloakId("keycloakId");
            verify(tenantCommonRepository, times(0)).findByEmail(any());
            verify(tenantService, times(0)).registerFromKeycloakUser(any(), any(), any());

            assertThat(result).isInstanceOf(Tenant.class);
            assertThat(((Tenant) result).getId()).isEqualTo(1L);

        }

        @Test
        void shouldReturnTenantByEmail() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("sub", "keycloakId");
            claimsMap.put("email", "test@test.fr");
            claimsMap.put("email_verified", false);
            claimsMap.put("france-connect", false);

            var keycloakUser = KeycloakUser.builder()
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .emailVerified(true)
                    .build();

            var tenant = Tenant.builder().id(1L).build();

            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_dossier"));
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, authorities)));

            when(tenantCommonRepository.findByKeycloakId("keycloakId")).thenReturn(null);
            when(tenantCommonRepository.findByEmail("test@test.fr")).thenReturn(Optional.of(tenant));

            var methodToTest = authenticationFacade.getClass().getDeclaredMethod("findOrCreateTenant", KeycloakUser.class, AcquisitionData.class);
            methodToTest.setAccessible(true);
            var result = methodToTest.invoke(authenticationFacade, keycloakUser, null);

            verify(tenantCommonRepository, times(1)).findByKeycloakId("keycloakId");
            verify(tenantCommonRepository, times(1)).findByEmail("test@test.fr");
            verify(tenantService, times(0)).registerFromKeycloakUser(any(), any(), any());

            assertThat(result).isInstanceOf(Tenant.class);
            assertThat(((Tenant) result).getId()).isEqualTo(1L);

        }
    }

    @Nested
    class KeycloakAndTenantMatchTest {
        record MatchParameters(Tenant tenant, KeycloakUser keycloakUser, boolean result) {
        }

        static List<Arguments> provideMatchParameters() {
            return List.of(
                    Arguments.of(
                            Named.of("Keycloak user and tenant match", new MatchParameters(
                                    Tenant.builder().keycloakId("keycloakId").build(),
                                    KeycloakUser.builder().keycloakId("keycloakId").build(),
                                    true
                            ))
                    ),
                    Arguments.of(
                            Named.of("id not the same", new MatchParameters(
                                    Tenant.builder().keycloakId("keycloakId1").build(),
                                    KeycloakUser.builder().keycloakId("keycloakId2").build(),
                                    false
                            ))
                    ),
                    Arguments.of(
                            Named.of("email not the same", new MatchParameters(
                                    Tenant.builder().keycloakId("keycloakId").email("test@test.fr").build(),
                                    KeycloakUser.builder().keycloakId("keycloakId").email("test2@test.fr").build(),
                                    false
                            ))
                    ),
                    Arguments.of(
                            Named.of("france connect not the same", new MatchParameters(
                                    Tenant.builder()
                                            .keycloakId("keycloakId")
                                            .email("test@test.fr")
                                            .franceConnect(false)
                                            .build(),
                                    KeycloakUser.builder()
                                            .keycloakId("keycloakId")
                                            .email("test@test.fr")
                                            .franceConnect(true)
                                            .build(),
                                    false
                            ))
                    ),
                    // Todo : The result should be false because the names are not identical
                    Arguments.of(
                            Named.of("is france connected but name are different and last name the same", new MatchParameters(
                                    Tenant.builder()
                                            .keycloakId("keycloakId")
                                            .email("test@test.fr")
                                            .franceConnect(true)
                                            .firstName("test")
                                            .lastName("test2")
                                            .build(),
                                    KeycloakUser.builder()
                                            .keycloakId("keycloakId")
                                            .email("test@test.fr")
                                            .franceConnect(true)
                                            .givenName("test")
                                            .familyName("test")
                                            .build(),
                                    true
                            ))
                    ),
                    Arguments.of(
                            Named.of("is france connected but name are different and last name as well", new MatchParameters(
                                    Tenant.builder()
                                            .keycloakId("keycloakId")
                                            .email("test@test.fr")
                                            .franceConnect(true)
                                            .firstName("test")
                                            .lastName("test")
                                            .build(),
                                    KeycloakUser.builder()
                                            .keycloakId("keycloakId")
                                            .email("test@test.fr")
                                            .franceConnect(true)
                                            .givenName("test2")
                                            .familyName("test2")
                                            .build(),
                                    false
                            ))
                    ),
                    Arguments.of(
                            Named.of("is not france connected and name are different", new MatchParameters(
                                    Tenant.builder()
                                            .keycloakId("keycloakId")
                                            .email("test@test.fr")
                                            .franceConnect(false)
                                            .firstName("test")
                                            .build(),
                                    KeycloakUser.builder()
                                            .keycloakId("keycloakId")
                                            .email("test@test.fr")
                                            .franceConnect(false)
                                            .givenName("test2")
                                            .build(),
                                    true
                            ))
                    )
            );
        }

        @ParameterizedTest
        @MethodSource("provideMatchParameters")
        void parametrizedTests(MatchParameters matchParameters) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

            var methodToTest = authenticationFacade.getClass().getDeclaredMethod("matches", Tenant.class, KeycloakUser.class);
            methodToTest.setAccessible(true);
            var result = methodToTest.invoke(authenticationFacade, matchParameters.tenant, matchParameters.keycloakUser);

            assertThat(result).isInstanceOf(Boolean.class);
            assertThat((Boolean) result).isEqualTo(matchParameters.result);

        }


    }

    /*
     * We use introspection to test private method to simplify the test
     */
    @Nested
    class SynchroniseTenantTest {

        @Test
        void shouldNotUpdateTenant() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            var tenant = Tenant.builder()
                    .id(1L)
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .build();

            var keycloakUser = KeycloakUser.builder()
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .build();

            var methodToTest = authenticationFacade.getClass().getDeclaredMethod("synchronizeTenant", Tenant.class, KeycloakUser.class);
            methodToTest.setAccessible(true);
            var result = methodToTest.invoke(authenticationFacade, tenant, keycloakUser);

            verify(tenantCommonRepository, times(0)).saveAndFlush(any());

            assertThat(tenant).isEqualTo(result);
        }

        @Test
        void shouldNotUpdateTenantBecauseEmailAreNotTheSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            var tenant = Tenant.builder()
                    .id(1L)
                    .keycloakId("keycloakId")
                    .email("test2@test.fr")
                    .build();

            var keycloakUser = KeycloakUser.builder()
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .build();

            var methodToTest = authenticationFacade.getClass().getDeclaredMethod("synchronizeTenant", Tenant.class, KeycloakUser.class);
            methodToTest.setAccessible(true);
            var result = methodToTest.invoke(authenticationFacade, tenant, keycloakUser);

            verify(tenantCommonRepository, times(0)).saveAndFlush(any());

            assertThat(tenant).isEqualTo(result);
            assertThat(((Tenant) result).getWarningMessage()).isNotBlank();
        }

        @Test
        void shouldUpdateTenantWithNewKeycloakId() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            var tenant = Tenant.builder()
                    .id(1L)
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .build();

            var keycloakUser = KeycloakUser.builder()
                    .keycloakId("keycloakId2")
                    .email("test@test.fr")
                    .build();

            // make that the method return the invocation parameter
            when(tenantCommonRepository.saveAndFlush(any())).thenAnswer((Answer<Tenant>) invocation -> invocation.getArgument(0));

            var methodToTest = authenticationFacade.getClass().getDeclaredMethod("synchronizeTenant", Tenant.class, KeycloakUser.class);
            methodToTest.setAccessible(true);
            var result = methodToTest.invoke(authenticationFacade, tenant, keycloakUser);

            verify(tenantStatusService, times(1)).updateTenantStatus(tenant);
            verify(tenantCommonRepository, times(1)).saveAndFlush(any());

            assertThat(tenant).isEqualTo(result);
            assertThat(((Tenant) result).getKeycloakId()).isEqualTo(keycloakUser.getKeycloakId());
        }

        // According to the code this test should passe but check comment on matches method
        @Disabled
        @Test
        void shouldUpdateTenantAndResetDocumentsError() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            var tenant = Tenant.builder()
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .franceConnect(true)
                    .firstName("test")
                    .lastName("test2")
                    .build();
            var keycloakUser = KeycloakUser.builder()
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .franceConnect(true)
                    .givenName("test")
                    .familyName("test")
                    .build();


            // make that the method return the invocation parameter
            when(tenantCommonRepository.saveAndFlush(any())).thenAnswer((Answer<Tenant>) invocation -> invocation.getArgument(0));

            var methodToTest = authenticationFacade.getClass().getDeclaredMethod("synchronizeTenant", Tenant.class, KeycloakUser.class);
            methodToTest.setAccessible(true);
            var result = methodToTest.invoke(authenticationFacade, tenant, keycloakUser);

            verify(tenantStatusService, times(1)).updateTenantStatus(tenant);
            verify(tenantCommonRepository, times(1)).saveAndFlush(any());
            verify(documentService, times(1)).resetValidatedOrInProgressDocumentsAccordingCategories(any(), any());

            assertThat(tenant).isEqualTo(result);
            assertThat(((Tenant) result).getKeycloakId()).isEqualTo(keycloakUser.getKeycloakId());
        }

        @Test
        void shouldUpdateTenantAndResetDocuments() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            var tenant = Tenant.builder()
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .franceConnect(true)
                    .firstName("test2")
                    .lastName("test2")
                    .build();
            var keycloakUser = KeycloakUser.builder()
                    .keycloakId("keycloakId")
                    .email("test@test.fr")
                    .franceConnect(true)
                    .givenName("test")
                    .familyName("test")
                    .build();


            // make that the method return the invocation parameter
            when(tenantCommonRepository.saveAndFlush(any())).thenAnswer((Answer<Tenant>) invocation -> invocation.getArgument(0));

            var methodToTest = authenticationFacade.getClass().getDeclaredMethod("synchronizeTenant", Tenant.class, KeycloakUser.class);
            methodToTest.setAccessible(true);
            var result = methodToTest.invoke(authenticationFacade, tenant, keycloakUser);

            verify(tenantStatusService, times(1)).updateTenantStatus(tenant);
            verify(tenantCommonRepository, times(1)).saveAndFlush(any());
            verify(documentService, times(1)).resetValidatedOrInProgressDocumentsAccordingCategories(any(), any());

            assertThat(tenant).isEqualTo(result);
            assertThat(((Tenant) result).getKeycloakId()).isEqualTo(keycloakUser.getKeycloakId());
        }

    }

    @Nested
    class GetFranceConnectLinkTest {

        @Test
        void shouldReturnFranceConnectLink() throws URISyntaxException {

            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("sub", "keycloakId");
            claimsMap.put("email", "test@test.fr");
            claimsMap.put("azp", "testAzp");
            claimsMap.put("session_state", "testToken");

            var jwt = getDummyJwtWithCustomClaims(claimsMap);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_dossier"));
            SecurityContextHolder.setContext(new SecurityContextImpl(new JwtAuthenticationToken(jwt, authorities)));

            var result = authenticationFacade.getFranceConnectLink("redirectUri");

            var uriResult = new URI(result);
            var actualBaseUrl = uriResult.getScheme() + "://" + uriResult.getHost() + ":" + uriResult.getPort() + uriResult.getPath();

            assertThat(actualBaseUrl).isEqualTo("http://localhost:8085/auth/realms/test/broker/oidc/link");

            var params = UriComponentsBuilder.fromUri(uriResult).build().getQueryParams().toSingleValueMap();
            assertThat(params.size()).isEqualTo(4);
            assertThat(params.get("nonce")).isNotBlank();
            assertThat(params.get("hash")).isNotBlank();
            assertThat(params.get("client_id")).isEqualTo("testAzp");
            assertThat(params.get("redirect_uri")).isEqualTo("redirectUri");
        }

    }
}
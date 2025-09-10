package fr.dossierfacile.api.front.dfc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.MethodSecurityConfig;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.mapper.TenantMapperImpl;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.model.dfc.apartment_sharing.ApartmentSharingModel;
import fr.dossierfacile.api.front.model.dfc.apartment_sharing.TenantModel;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.mapper.VersionedCategoriesMapper;
import fr.dossierfacile.parameterizedtest.ArgumentBuilder;
import fr.dossierfacile.parameterizedtest.ControllerParameter;
import fr.dossierfacile.parameterizedtest.ParameterizedTestHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static fr.dossierfacile.authentification.JwtFactoryKt.getDummyJwtWithCustomClaims;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(DfcTenantController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {"application.api.version= 4", "dossierfacile.common.global.exception.handler=true"})
@ContextConfiguration(classes = {TestApplication.class, TenantMapperImpl.class, VersionedCategoriesMapper.class, ResourceServerConfig.class, MethodSecurityConfig.class, GlobalExceptionHandler.class})
class DfcTenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserApiService userApiService;

    @MockitoBean
    private TenantPermissionsService tenantPermissionsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static ObjectMapper mapper = new ObjectMapper();

    // Référence statique à l’instance courante du test
    private static DfcTenantControllerTest self;

    @PostConstruct
    void setSelf() {
        self = this;
    }

    @BeforeEach
    void beforeEach() {
        reset(authenticationFacade, tenantService, userService, userApiService, tenantPermissionsService);
    }

    @Nested
    class ProfilePartnerTests {

        static List<Arguments> provideProfilePartnerParameters() throws Exception {
            var clientClaimsMap = new HashMap<String, Object>();
            clientClaimsMap.put("client_id", "client_id");
            var jwtWithClientClaims = getDummyJwtWithCustomClaims(clientClaimsMap);

            var claimsMap = new HashMap<String, Object>();
            clientClaimsMap.put("azp", "client_id");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDfc = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dfc")).jwt(jwtWithClientClaims);
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithClaimsMap = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dfc")).jwt(jwt);
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithWrongScope = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier")).jwt(jwtWithClientClaims);

            var apartmentSharing = ApartmentSharing.builder()
                    .id(1L)
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .email("test@test.fr")
                    .apartmentSharing(apartmentSharing)
                    .build();

            UserApi userApi = UserApi.builder()
                    .id(1L)
                    .name("test")
                    .build();

            apartmentSharing.setTenants(List.of(tenant));

            var keycloakUser = KeycloakUser.builder()
                    .keycloakId("keycloak_id")
                    .email("test@test.fr")
                    .build();

            var expectedTenantConnectModel = ConnectedTenantModel.builder()
                    .connectedTenantId(1L)
                    .apartmentSharing(ApartmentSharingModel.builder()
                            .id(1L)
                            .tenants(List.of(TenantModel.builder()
                                    .id(1L)
                                    .email("test@test.fr")
                                    .build()
                            ))
                            .build()
                    ).build();

            var expectedJson = mapper.writeValueAsString(expectedTenantConnectModel);

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when no jwt is passed",
                            new ControllerParameter<>(
                                    null,
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when wrong scope is passed",
                            new ControllerParameter<>(
                                    null,
                                    403,
                                    jwtWithWrongScope,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when from client jwt",
                            new ControllerParameter<>(
                                    null,
                                    403,
                                    jwtTokenWithDfc,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 and link tenant to partner by id",
                            new ControllerParameter<>(
                                    null,
                                    200,
                                    jwtWithClaimsMap,
                                    (v) -> {
                                        when(self.authenticationFacade.getKeycloakClientId()).thenReturn("client_id");
                                        when(self.authenticationFacade.getKeycloakUserId()).thenReturn("keycloak_id");
                                        when(self.tenantService.findByKeycloakId("keycloak_id")).thenReturn(tenant);
                                        when(self.userApiService.findByName("client_id")).thenReturn(Optional.of(userApi));
                                        return v;
                                    },
                                    List.of(
                                            content().json(expectedJson)
                                    ),
                                    (v) -> {
                                        verify(self.tenantService, times(0)).findByEmail(any());
                                        verify(self.userService, times(1)).linkTenantToPartner(tenant, "client_id", null);
                                        return v;
                                    }
                            )
                    ),
                    Pair.of("Should respond 200 and link tenant to partner by email",
                            new ControllerParameter<>(
                                    null,
                                    200,
                                    jwtWithClaimsMap,
                                    (v) -> {
                                        when(self.authenticationFacade.getKeycloakClientId()).thenReturn("client_id");
                                        when(self.tenantService.findByKeycloakId("keycloak_id")).thenReturn(null);
                                        when(self.authenticationFacade.getUserEmail()).thenReturn("test@test.fr");
                                        when(self.tenantService.findByEmail("test@test.fr")).thenReturn(Optional.of(tenant));
                                        when(self.authenticationFacade.getKeycloakUserId()).thenReturn("keycloak_id");
                                        when(self.userApiService.findByName("client_id")).thenReturn(Optional.of(userApi));
                                        return v;
                                    },
                                    List.of(
                                            content().json(expectedJson)
                                    ),
                                    (v) -> {
                                        verify(self.tenantService, times(1)).findByKeycloakId("keycloak_id");
                                        verify(self.tenantService, times(1)).findByEmail("test@test.fr");
                                        verify(self.userService, times(1)).linkTenantToPartner(tenant, "client_id", null);
                                        return v;
                                    }
                            )
                    ),
                    Pair.of("Should respond 200 register user",
                            new ControllerParameter<>(
                                    null,
                                    200,
                                    jwtWithClaimsMap,
                                    (v) -> {
                                        when(self.authenticationFacade.getKeycloakClientId()).thenReturn("client_id");
                                        when(self.tenantService.findByKeycloakId("keycloak_id")).thenReturn(null);
                                        when(self.authenticationFacade.getUserEmail()).thenReturn("test@test.fr");
                                        when(self.tenantService.findByEmail("test@test.fr")).thenReturn(Optional.empty());
                                        when(self.authenticationFacade.getKeycloakUserId()).thenReturn("keycloak_id");
                                        when(self.authenticationFacade.getKeycloakUser()).thenReturn(keycloakUser);
                                        when(self.tenantService.registerFromKeycloakUser(keycloakUser, "client_id", null)).thenReturn(tenant);
                                        when(self.userApiService.findByName("client_id")).thenReturn(Optional.of(userApi));
                                        return v;
                                    },
                                    List.of(
                                            content().json(expectedJson)
                                    ),
                                    (v) -> {
                                        verify(self.tenantService, times(1)).findByKeycloakId("keycloak_id");
                                        verify(self.tenantService, times(1)).findByEmail("test@test.fr");
                                        verify(self.tenantService, times(1)).registerFromKeycloakUser(keycloakUser, "client_id", null);
                                        verify(self.userService, times(0)).linkTenantToPartner(any(), any(), any());
                                        return v;
                                    }
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideProfilePartnerParameters")
        void parameterizedTests(ControllerParameter<Void> parameter) throws Exception {

            var mockMvcRequestBuilder = get("/dfc/tenant/profile")
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }

    @Nested
    class LogoutTests {

        static List<Arguments> provideLogoutParameters() {

            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDfc = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dfc"));
            SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtTokenWithDossier = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier"));

            return ArgumentBuilder.buildListOfArguments(
                    Pair.of("Should respond 401 when no jwt is passed",
                            new ControllerParameter<>(
                                    null,
                                    401,
                                    null,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when jwt with wrong scope is passed",
                            new ControllerParameter<>(
                                    null,
                                    403,
                                    jwtTokenWithDossier,
                                    null,
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when jwt is passed",
                            new ControllerParameter<>(
                                    null,
                                    200,
                                    jwtTokenWithDfc,
                                    (v) -> {
                                        when(self.authenticationFacade.getKeycloakUserId()).thenReturn("keycloak_id");
                                        return v;
                                    },
                                    List.of(
                                            content().bytes(new byte[0])
                                    )
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideLogoutParameters")
        void parameterizedTests(ControllerParameter<Void> parameter) throws Exception {

            var mockMvcRequestBuilder = post("/dfc/tenant/logout")
                    .contentType("application/json");

            ParameterizedTestHelper.runControllerTest(
                    mockMvc,
                    mockMvcRequestBuilder,
                    parameter
            );
        }
    }
}
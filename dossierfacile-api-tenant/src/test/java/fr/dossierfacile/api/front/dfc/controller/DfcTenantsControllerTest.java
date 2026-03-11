package fr.dossierfacile.api.front.dfc.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.api.front.model.dfc.apartment_sharing.ApartmentSharingModel;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.parameterizedtest.ArgumentBuilder;
import fr.dossierfacile.parameterizedtest.ControllerParameter;
import fr.dossierfacile.parameterizedtest.ParameterizedTestHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import static fr.dossierfacile.authentification.JwtFactoryKt.getDummyJwtWithCustomClaims;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DfcTenantsController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestApplication.class, ResourceServerConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"dossierfacile.common.global.exception.handler=true"})
class DfcTenantsControllerTest {

    private static final Long TENANT_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientAuthenticationFacade clientAuthenticationFacade;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private TenantPermissionsService tenantPermissionsService;

    @MockitoBean
    private TenantMapper tenantMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static DfcTenantsControllerTest self;

    @PostConstruct
    void setSelf() {
        self = this;
    }

    @BeforeEach
    void beforeEach() {
        reset(clientAuthenticationFacade, tenantService, tenantPermissionsService, tenantMapper);
    }

    @Nested
    class ListTenantsTests {

        @Test
        void should_add_response_metadata() throws Exception {
            UserApi userApi = UserApi.builder().id(1L).name("partner").build();
            when(self.clientAuthenticationFacade.getClient()).thenReturn(userApi);
            when(self.tenantService.findTenantUpdateByLastUpdateAndPartner(any(), eq(userApi), anyLong(), anyBoolean(), anyBoolean()))
                    .thenReturn(Collections.emptyList());

            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("client_id", "partner");
            var request = get("/dfc/api/v1/tenants")
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_dfc"))
                            .jwt(getDummyJwtWithCustomClaims(claimsMap)))
                    .queryParam("after", "2020-01-31T10:30:00.000-05:00")
                    .queryParam("limit", "10")
                    .queryParam("includeDeleted", "true");

            String contentAsString = mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse().getContentAsString();

            assertThat(contentAsString).isEqualToIgnoringNewLines("""
                    {"data":[],"metadata":{"limit":10,"resultCount":0,"nextLink":"/dfc/api/v1/tenants?limit=10&after=2020-01-31T10:30&includeDeleted=true&includeRevoked=false"}}""");
        }
    }

    @Nested
    class GetTenantByIdTests {

        static List<Arguments> provideGetTenantByIdParameters() {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("client_id", "partner-client");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);

            var jwtTokenWithDfc = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dfc")).jwt(jwt);
            var jwtWithWrongScope = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier")).jwt(jwt);

            Tenant tenant = Tenant.builder()
                    .id(TENANT_ID)
                    .email("tenant@example.com")
                    .build();

            UserApi userApi = UserApi.builder()
                    .id(1L)
                    .name("partner-client")
                    .build();

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
                    Pair.of("Should respond 404 when tenant is not found",
                            new ControllerParameter<>(
                                    null,
                                    404,
                                    jwtTokenWithDfc,
                                    (v) -> {
                                        when(self.tenantService.findById(TENANT_ID)).thenReturn(null);
                                        when(self.clientAuthenticationFacade.getClient()).thenReturn(userApi);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when partner is not linked to tenant",
                            new ControllerParameter<>(
                                    null,
                                    403,
                                    jwtTokenWithDfc,
                                    (v) -> {
                                        when(self.tenantService.findById(TENANT_ID)).thenReturn(tenant);
                                        when(self.clientAuthenticationFacade.getKeycloakClientId()).thenReturn("partner-client");
                                        when(self.clientAuthenticationFacade.getClient()).thenReturn(userApi);
                                        when(self.tenantPermissionsService.clientCanAccess("partner-client", TENANT_ID)).thenReturn(false);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when partner is linked to tenant",
                            new ControllerParameter<>(
                                    null,
                                    200,
                                    jwtTokenWithDfc,
                                    (v) -> {
                                        when(self.tenantService.findById(TENANT_ID)).thenReturn(tenant);
                                        when(self.clientAuthenticationFacade.getKeycloakClientId()).thenReturn("partner-client");
                                        when(self.clientAuthenticationFacade.getClient()).thenReturn(userApi);
                                        when(self.tenantPermissionsService.clientCanAccess("partner-client", TENANT_ID)).thenReturn(true);
                                        when(self.tenantMapper.toTenantModelDfc(eq(tenant), eq(userApi)))
                                                .thenReturn(ConnectedTenantModel.builder()
                                                        .connectedTenantId(TENANT_ID)
                                                        .apartmentSharing(ApartmentSharingModel.builder().build())
                                                        .build());
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideGetTenantByIdParameters")
        void parameterizedTests(ControllerParameter<Void> parameter) throws Exception {
            var mockMvcRequestBuilder = get("/dfc/api/v1/tenants/{tenantId}", TENANT_ID);
            ParameterizedTestHelper.runControllerTest(mockMvc, mockMvcRequestBuilder, parameter);
        }
    }
}

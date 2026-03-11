package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(ApiPartnerApartmentSharingController.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestApplication.class, ResourceServerConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"dossierfacile.common.global.exception.handler=true"})
class ApiPartnerApartmentSharingControllerTest {

    private static final Long APARTMENT_SHARING_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApartmentSharingService apartmentSharingService;

    @MockitoBean
    private UserApiService userApiService;

    @MockitoBean
    private ClientAuthenticationFacade clientAuthenticationFacade;

    @MockitoBean
    private ApplicationFullMapper applicationFullMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static ApiPartnerApartmentSharingControllerTest self;

    @PostConstruct
    void setSelf() {
        self = this;
    }

    @BeforeEach
    void beforeEach() {
        reset(apartmentSharingService, userApiService, clientAuthenticationFacade, applicationFullMapper);
    }

    @Nested
    class GetApartmentSharingTests {

        static List<Arguments> provideGetApartmentSharingParameters() {
            var claimsMap = new HashMap<String, Object>();
            claimsMap.put("client_id", "partner-client");
            var jwt = getDummyJwtWithCustomClaims(claimsMap);

            var jwtTokenWithApiPartner = jwt().authorities(new SimpleGrantedAuthority("SCOPE_api-partner")).jwt(jwt);
            var jwtWithWrongScope = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dossier")).jwt(jwt);

            Tenant tenant = Tenant.builder().id(100L).build();
            ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                    .id(APARTMENT_SHARING_ID)
                    .tenants(List.of(tenant))
                    .build();

            UserApi userApi = UserApi.builder()
                    .id(1L)
                    .name("partner-client")
                    .build();

            ApplicationModel applicationModel = ApplicationModel.builder()
                    .id(APARTMENT_SHARING_ID)
                    .tenants(Collections.emptyList())
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
                    Pair.of("Should respond 404 when apartment sharing is not found",
                            new ControllerParameter<>(
                                    null,
                                    404,
                                    jwtTokenWithApiPartner,
                                    (v) -> {
                                        when(self.apartmentSharingService.findById(APARTMENT_SHARING_ID)).thenReturn(Optional.empty());
                                        when(self.clientAuthenticationFacade.getClient()).thenReturn(userApi);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 403 when partner is not linked to any tenant of apartment sharing",
                            new ControllerParameter<>(
                                    null,
                                    403,
                                    jwtTokenWithApiPartner,
                                    (v) -> {
                                        when(self.apartmentSharingService.findById(APARTMENT_SHARING_ID)).thenReturn(Optional.of(apartmentSharing));
                                        when(self.clientAuthenticationFacade.getClient()).thenReturn(userApi);
                                        when(self.userApiService.anyTenantIsLinked(eq(userApi), any())).thenReturn(false);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    ),
                    Pair.of("Should respond 200 when partner is linked to at least one tenant",
                            new ControllerParameter<>(
                                    null,
                                    200,
                                    jwtTokenWithApiPartner,
                                    (v) -> {
                                        when(self.apartmentSharingService.findById(APARTMENT_SHARING_ID)).thenReturn(Optional.of(apartmentSharing));
                                        when(self.clientAuthenticationFacade.getClient()).thenReturn(userApi);
                                        when(self.userApiService.anyTenantIsLinked(eq(userApi), any())).thenReturn(true);
                                        when(self.applicationFullMapper.toApplicationModel(any(), eq(userApi))).thenReturn(applicationModel);
                                        return v;
                                    },
                                    Collections.emptyList()
                            )
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("provideGetApartmentSharingParameters")
        void parameterizedTests(ControllerParameter<Void> parameter) throws Exception {
            var mockMvcRequestBuilder = get("/api-partner/apartmentSharing/{id}", APARTMENT_SHARING_ID);
            ParameterizedTestHelper.runControllerTest(mockMvc, mockMvcRequestBuilder, parameter);
        }
    }
}

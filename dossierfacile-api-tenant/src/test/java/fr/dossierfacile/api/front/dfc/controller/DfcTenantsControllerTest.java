package fr.dossierfacile.api.front.dfc.controller;

import fr.dossierfacile.api.front.TestApplication;
import fr.dossierfacile.api.front.config.ResourceServerConfig;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.config.GlobalExceptionHandler;
import fr.dossierfacile.api.front.model.dfc.apartment_sharing.ApartmentSharingModel;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static fr.dossierfacile.authentification.JwtFactoryKt.getDummyJwtWithCustomClaims;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    private DocumentRepository documentRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

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

    @Nested
    class DownloadDocumentTests {

        private static final String DOC_UUID = "doc-uuid-123";
        private static final Long APARTMENT_SHARING_ID = 77L;
        private static final String URL = "/dfc/api/v1/tenants/{tenantId}/documents/{documentUuid}";

        private org.springframework.test.web.servlet.request.RequestPostProcessor jwtWithBothScopes() {
            var claims = new HashMap<String, Object>();
            claims.put("client_id", "partner-client");
            return jwt().authorities(
                            new SimpleGrantedAuthority("SCOPE_dfc"),
                            new SimpleGrantedAuthority("SCOPE_dfc-documents"))
                    .jwt(getDummyJwtWithCustomClaims(claims));
        }

        private Tenant consentedTenant() {
            ApartmentSharing apartmentSharing = ApartmentSharing.builder().id(APARTMENT_SHARING_ID).build();
            return Tenant.builder().id(TENANT_ID).apartmentSharing(apartmentSharing).build();
        }

        private Document downloadableDocument() {
            return Document.builder()
                    .id(10L)
                    .name(DOC_UUID)
                    .documentSubCategory(DocumentSubCategory.OTHER_IDENTIFICATION)
                    .watermarkFile(new StorageFile())
                    .build();
        }

        @Test
        void should_respond_403_when_dfc_documents_scope_missing() throws Exception {
            var claims = new HashMap<String, Object>();
            claims.put("client_id", "partner-client");
            var jwtDfcOnly = jwt().authorities(new SimpleGrantedAuthority("SCOPE_dfc"))
                    .jwt(getDummyJwtWithCustomClaims(claims));

            mockMvc.perform(get(URL, TENANT_ID, DOC_UUID).with(jwtDfcOnly))
                    .andExpect(status().isForbidden());
        }

        @Test
        void should_respond_403_when_partner_not_consented() throws Exception {
            when(self.tenantService.findById(TENANT_ID)).thenReturn(consentedTenant());
            when(self.clientAuthenticationFacade.getKeycloakClientId()).thenReturn("partner-client");
            when(self.tenantPermissionsService.clientCanAccess("partner-client", TENANT_ID)).thenReturn(false);

            mockMvc.perform(get(URL, TENANT_ID, DOC_UUID).with(jwtWithBothScopes()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void should_respond_404_when_document_not_in_apartment_sharing() throws Exception {
            when(self.tenantService.findById(TENANT_ID)).thenReturn(consentedTenant());
            when(self.clientAuthenticationFacade.getKeycloakClientId()).thenReturn("partner-client");
            when(self.tenantPermissionsService.clientCanAccess("partner-client", TENANT_ID)).thenReturn(true);
            // Document does not belong to the consented tenant's apartment sharing -> anti-IDOR
            when(self.documentRepository.findByNameForApartmentSharing(DOC_UUID, APARTMENT_SHARING_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get(URL, TENANT_ID, DOC_UUID).with(jwtWithBothScopes()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void should_respond_200_and_pdf_when_authorized() throws Exception {
            Document document = downloadableDocument();
            when(self.tenantService.findById(TENANT_ID)).thenReturn(consentedTenant());
            when(self.clientAuthenticationFacade.getKeycloakClientId()).thenReturn("partner-client");
            when(self.tenantPermissionsService.clientCanAccess("partner-client", TENANT_ID)).thenReturn(true);
            when(self.documentRepository.findByNameForApartmentSharing(DOC_UUID, APARTMENT_SHARING_ID))
                    .thenReturn(Optional.of(document));
            when(self.fileStorageService.download(document.getWatermarkFile()))
                    .thenReturn(new ByteArrayInputStream("%PDF-1.4 fake".getBytes()));

            mockMvc.perform(get(URL, TENANT_ID, DOC_UUID).with(jwtWithBothScopes()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }
    }
}

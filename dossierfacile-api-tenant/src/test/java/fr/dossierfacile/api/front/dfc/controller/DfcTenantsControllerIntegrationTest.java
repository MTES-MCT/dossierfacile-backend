package fr.dossierfacile.api.front.dfc.controller;

import fr.dossierfacile.api.front.mapper.TenantMapperImpl;
import fr.dossierfacile.api.front.register.RegisterFactory;
import fr.dossierfacile.api.front.repository.JpaTestApplication;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.TenantServiceImpl;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = JpaTestApplication.class)
@Import({DfcTenantsController.class, TenantServiceImpl.class, TenantMapperImpl.class})
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "application.base.url=https://api.test.com",
        "tenant.base.url=https://test.com",
        "api.failed.rules.min.level=WARN"
})
@Transactional
class DfcTenantsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @PersistenceContext
    private EntityManager em;

    @MockitoBean
    private ClientAuthenticationFacade clientAuthenticationFacade;
    @MockitoBean
    private TenantPermissionsService tenantPermissionsService;
    @MockitoBean
    private ConfirmationTokenService confirmationTokenService;
    @MockitoBean
    private LogService logService;
    @MockitoBean
    private MailService mailService;
    @MockitoBean
    private PartnerCallBackService partnerCallBackService;
    @MockitoBean
    private RegisterFactory registerFactory;
    @MockitoBean
    private KeycloakService keycloakService;
    @MockitoBean
    private UserApiService userApiService;
    @MockitoBean
    private TenantMapperForMail tenantMapperForMail;

    private Long tenantId;
    private String expectedDocumentUrl;
    private String expectedToken;
    private String expectedPublicToken;
    private String expectedDossierUrl;
    private String expectedDossierPdfUrl;

    @BeforeEach
    void setUp() {
        UserApi userApi = UserApi.builder().name("partner-test").build();
        em.persist(userApi);

        ApartmentSharing sharing = ApartmentSharing.builder()
                .applicationType(ApplicationType.ALONE)
                .build();
        em.persist(sharing);

        Tenant tenant = Tenant.builder()
                .email("tenant@test.com")
                .apartmentSharing(sharing)
                .tenantType(TenantType.CREATE)
                .status(TenantFileStatus.TO_PROCESS)
                .franceConnect(false)
                .build();
        em.persist(tenant);
        tenantId = tenant.getId();

        sharing.setTenants(List.of(tenant));

        Document document = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .tenant(tenant)
                .build();
        em.persist(document);

        tenant.setDocuments(List.of(document));

        UUID fullLinkToken = UUID.randomUUID();
        ApartmentSharingLink fullLink = ApartmentSharingLink.builder()
                .apartmentSharing(sharing)
                .linkType(ApartmentSharingLinkType.PARTNER)
                .partnerId(userApi.getId())
                .fullData(true)
                .token(fullLinkToken)
                .disabled(false)
                .deleted(false)
                .build();
        em.persist(fullLink);

        UUID restrictedLinkToken = UUID.randomUUID();
        ApartmentSharingLink restrictedLink = ApartmentSharingLink.builder()
                .apartmentSharing(sharing)
                .linkType(ApartmentSharingLinkType.PARTNER)
                .partnerId(userApi.getId())
                .fullData(false)
                .token(restrictedLinkToken)
                .disabled(false)
                .deleted(false)
                .build();
        em.persist(restrictedLink);

        sharing.setApartmentSharingLinks(List.of(fullLink, restrictedLink));
        em.flush();

        // H2 doesn't handle well Storagefile providers (character varying[]), so we only set it in memory, not in db
        document.setWatermarkFile(StorageFile.builder().name("watermark.pdf").build());

        when(clientAuthenticationFacade.getKeycloakClientId()).thenReturn("partner-test");
        when(clientAuthenticationFacade.getClient()).thenReturn(userApi);
        when(tenantPermissionsService.clientCanAccess(any(), any())).thenReturn(true);

        expectedDocumentUrl = "https://api.test.com/api/application/links/"
                + fullLinkToken + "/documents/" + document.getName();
        
        expectedToken = fullLinkToken.toString();
        expectedPublicToken = restrictedLinkToken.toString();

        expectedDossierUrl = "https://test.com/file/" + fullLinkToken;
        expectedDossierPdfUrl = "https://api.test.com/api/application/fullPdf/" + fullLinkToken;
    }

    @Test
    void shouldDisplayLinkPathToDocumentsWhenDossierIsNotValidated() throws Exception {
        mockMvc.perform(get("/dfc/api/v1/tenants/{id}", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartmentSharing.token").doesNotExist())
                .andExpect(jsonPath("$.apartmentSharing.tokenPublic").doesNotExist())
                .andExpect(jsonPath("$.apartmentSharing.dossierUrl").doesNotExist())
                .andExpect(jsonPath("$.apartmentSharing.dossierPdfUrl").doesNotExist())
                .andExpect(jsonPath("$.apartmentSharing.tenants[0].documents[0].name",
                        is(expectedDocumentUrl)));
    }

    @Test
    void shouldDisplayLinkPathToDocumentsWhenDossierIsValidated() throws Exception {
        Tenant tenant = em.find(Tenant.class, tenantId);
        tenant.setStatus(TenantFileStatus.VALIDATED);

        mockMvc.perform(get("/dfc/api/v1/tenants/{id}", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartmentSharing.token", is(expectedToken)))
                .andExpect(jsonPath("$.apartmentSharing.tokenPublic", is(expectedPublicToken)))
                .andExpect(jsonPath("$.apartmentSharing.dossierUrl", is(expectedDossierUrl)))
                .andExpect(jsonPath("$.apartmentSharing.dossierPdfUrl", is(expectedDossierPdfUrl)))
                .andExpect(jsonPath("$.apartmentSharing.tenants[0].documents[0].name",
                        is(expectedDocumentUrl)));
    }
}

package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.repository.JpaTestApplication;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.ApartmentSharingServiceImpl;
import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.mapper.ApplicationBasicMapper;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.mapper.ApplicationLightMapper;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import fr.dossierfacile.common.service.interfaces.LogService;
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

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = JpaTestApplication.class)
@Import({ApplicationController.class, ApartmentSharingServiceImpl.class})
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class ApplicationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @PersistenceContext
    private EntityManager em;

    // Actually exercised mocks
    @MockitoBean
    private FileStorageService fileStorageService;
    @MockitoBean
    private LinkLogService linkLogService;

    // Dependencies required for constructor injection but not invoked
    @MockitoBean
    private AuthenticationFacade authenticationFacade;
    @MockitoBean
    private Producer producer;
    @MockitoBean
    private ApplicationFullMapper applicationFullMapper;
    @MockitoBean
    private ApplicationLightMapper applicationLightMapper;
    @MockitoBean
    private ApplicationBasicMapper applicationBasicMapper;
    @MockitoBean
    private ApartmentSharingCommonService apartmentSharingCommonService;
    @MockitoBean
    private LogService logService;
    @MockitoBean
    private BruteForceProtectionService bruteForceProtectionService;

    private UUID validToken;
    private UUID disabledToken;
    private UUID deletedToken;
    private UUID expiredToken;
    private UUID otherSharingToken;

    private String tenantDocName;
    private String coTenantDocName;
    private String guarantorDocName;

    @BeforeEach
    void setUp() throws Exception {
        // Sharing 1: COUPLE with main tenant + co-tenant + guarantor
        ApartmentSharing sharing1 = ApartmentSharing.builder()
                .applicationType(ApplicationType.COUPLE)
                .build();
        em.persist(sharing1);

        Tenant mainTenant = Tenant.builder()
                .email("main@test.com")
                .apartmentSharing(sharing1)
                .tenantType(TenantType.CREATE)
                .build();
        em.persist(mainTenant);

        Tenant coTenant = Tenant.builder()
                .email("co@test.com")
                .apartmentSharing(sharing1)
                .tenantType(TenantType.JOIN)
                .build();
        em.persist(coTenant);

        Guarantor guarantor = Guarantor.builder()
                .firstName("Guarantor")
                .lastName("One")
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .tenant(mainTenant)
                .build();
        em.persist(guarantor);

        // Documents (no watermarkFile — fileStorageService.download() is mocked)
        Document tenantDoc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .tenant(mainTenant)
                .build();
        em.persist(tenantDoc);
        tenantDocName = tenantDoc.getName();

        Document coTenantDoc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .tenant(coTenant)
                .build();
        em.persist(coTenantDoc);
        coTenantDocName = coTenantDoc.getName();

        Document guarantorDoc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .guarantor(guarantor)
                .build();
        em.persist(guarantorDoc);
        guarantorDocName = guarantorDoc.getName();

        // Links for sharing 1
        validToken = UUID.randomUUID();
        em.persist(ApartmentSharingLink.builder()
                .apartmentSharing(sharing1)
                .token(validToken)
                .fullData(true)
                .disabled(false)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .build());

        disabledToken = UUID.randomUUID();
        em.persist(ApartmentSharingLink.builder()
                .apartmentSharing(sharing1)
                .token(disabledToken)
                .fullData(true)
                .disabled(true)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .build());

        deletedToken = UUID.randomUUID();
        em.persist(ApartmentSharingLink.builder()
                .apartmentSharing(sharing1)
                .token(deletedToken)
                .fullData(true)
                .disabled(false)
                .deleted(true)
                .linkType(ApartmentSharingLinkType.LINK)
                .build());

        expiredToken = UUID.randomUUID();
        em.persist(ApartmentSharingLink.builder()
                .apartmentSharing(sharing1)
                .token(expiredToken)
                .fullData(true)
                .disabled(false)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .expirationDate(LocalDateTime.of(2020, 1, 1, 0, 0))
                .build());

        // Sharing 2: isolated tenant (for cross-sharing isolation test)
        ApartmentSharing sharing2 = ApartmentSharing.builder()
                .applicationType(ApplicationType.ALONE)
                .build();
        em.persist(sharing2);

        Tenant otherTenant = Tenant.builder()
                .email("other@test.com")
                .apartmentSharing(sharing2)
                .tenantType(TenantType.CREATE)
                .build();
        em.persist(otherTenant);

        Document otherDoc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .tenant(otherTenant)
                .build();
        em.persist(otherDoc);

        otherSharingToken = UUID.randomUUID();
        em.persist(ApartmentSharingLink.builder()
                .apartmentSharing(sharing2)
                .token(otherSharingToken)
                .fullData(true)
                .disabled(false)
                .deleted(false)
                .linkType(ApartmentSharingLinkType.LINK)
                .build());

        em.flush();

        // Mock file download
        when(fileStorageService.download(any()))
                .thenReturn(new ByteArrayInputStream("PDF content".getBytes()));
    }

    @Test
    void shouldReturn200ForTenantDocument() throws Exception {
        mockMvc.perform(get("/api/application/links/{token}/documents/{documentName}",
                        validToken, tenantDocName))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void shouldReturn200ForCoTenantDocument() throws Exception {
        mockMvc.perform(get("/api/application/links/{token}/documents/{documentName}",
                        validToken, coTenantDocName))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200ForGuarantorDocument() throws Exception {
        mockMvc.perform(get("/api/application/links/{token}/documents/{documentName}",
                        validToken, guarantorDocName))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenLinkDisabled() throws Exception {
        mockMvc.perform(get("/api/application/links/{token}/documents/{documentName}",
                        disabledToken, tenantDocName))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenLinkDeleted() throws Exception {
        mockMvc.perform(get("/api/application/links/{token}/documents/{documentName}",
                        deletedToken, tenantDocName))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenLinkExpired() throws Exception {
        mockMvc.perform(get("/api/application/links/{token}/documents/{documentName}",
                        expiredToken, tenantDocName))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDocumentFromOtherSharing() throws Exception {
        mockMvc.perform(get("/api/application/links/{token}/documents/{documentName}",
                        otherSharingToken, tenantDocName))
                .andExpect(status().isNotFound());
    }
}

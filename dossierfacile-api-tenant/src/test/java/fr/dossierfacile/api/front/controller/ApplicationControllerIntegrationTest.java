package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.common.domain.service.MessagePublisher;
import fr.dossierfacile.api.front.application.usecase.application.CheckApplicationLinkUseCase;
import fr.dossierfacile.api.front.application.usecase.application.GetFullApplicationUseCase;
import fr.dossierfacile.api.front.application.usecase.application.GetLightApplicationUseCase;
import fr.dossierfacile.api.front.fixtures.ApplicationSeed;
import fr.dossierfacile.api.front.repository.JpaTestApplication;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.ApartmentSharingServiceImpl;
import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
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
        "spring.jpa.hibernate.ddl-auto=update"
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
    private MessagePublisher producer;
    @MockitoBean
    private ApplicationFullMapper applicationFullMapper;
    @MockitoBean
    private ApplicationLightMapper applicationLightMapper;
    @MockitoBean
    private ApartmentSharingCommonService apartmentSharingCommonService;
    @MockitoBean
    private TenantPermissionsService tenantPermissionsService;
    @MockitoBean
    private DocumentService documentService;
    @MockitoBean
    private LogService logService;
    @MockitoBean
    private BruteForceProtectionService bruteForceProtectionService;
    @MockitoBean
    private GetLightApplicationUseCase getLightApplicationUseCase;
    @MockitoBean
    private GetFullApplicationUseCase getFullApplicationUseCase;
    @MockitoBean
    private CheckApplicationLinkUseCase checkApplicationLinkUseCase;

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
        ApplicationSeed.Seed seed = ApplicationSeed.seed(em);

        validToken = seed.validToken();
        disabledToken = seed.disabledToken();
        deletedToken = seed.deletedToken();
        expiredToken = seed.expiredToken();
        otherSharingToken = seed.otherSharingToken();
        tenantDocName = seed.tenantDocName();
        coTenantDocName = seed.coTenantDocName();
        guarantorDocName = seed.guarantorDocName();

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

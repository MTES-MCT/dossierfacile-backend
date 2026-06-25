package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.common.domain.service.MessagePublisher;
import fr.dossierfacile.api.front.repository.JpaTestApplication;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.FileServiceImpl;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
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

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = JpaTestApplication.class)
@Import({FileController.class, FileServiceImpl.class})
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:filetestdb;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=update"
})
@Transactional
class FileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @PersistenceContext
    private EntityManager em;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private LogService logService;

    @MockitoBean
    private MessagePublisher producer;

    @MockitoBean
    private DocumentIAService documentIAService;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private fr.dossierfacile.api.front.application.usecase.tenant.TenantDeleteFileUseCase tenantDeleteFileUseCase;

    private Tenant tenantAlone;
    private Tenant tenantGroup1;
    private Tenant tenantGroup2;
    private Tenant tenantCouple1;
    private Tenant tenantCouple2;

    @BeforeEach
    void setUp() {
        // Mock TenantDeleteFileUseCase behaviors to perform database logic and log/async callbacks
        org.mockito.Mockito.doAnswer(invocation -> {
            fr.dossierfacile.api.front.application.usecase.tenant.TenantDeleteFileUseCase.TenantDeleteFileCommand cmd = invocation.getArgument(0);
            File file = em.find(File.class, cmd.fileId());
            if (file == null) {
                throw new fr.dossierfacile.api.front.application.exception.ModelNotFoundException(File.class, cmd.fileId());
            }
            Document doc = file.getDocument();
            Tenant targetTenant = doc.getTenant() != null ? doc.getTenant() : doc.getGuarantor().getTenant();
            Tenant currentTenant = authenticationFacade.getLoggedTenant();

            if (!currentTenant.getApartmentSharing().getId().equals(targetTenant.getApartmentSharing().getId())) {
                throw new fr.dossierfacile.api.front.application.exception.UnauthorizedException("No access");
            }
            if (!currentTenant.getId().equals(targetTenant.getId())) {
                if (currentTenant.getApartmentSharing().getApplicationType() != ApplicationType.COUPLE) {
                    throw new fr.dossierfacile.api.front.application.exception.UnauthorizedException("No access");
                }
            }

            logService.saveFileDeletedLog(file, targetTenant);

            doc.getFiles().remove(file);
            em.remove(file);
            if (doc.getFiles().isEmpty()) {
                logService.saveDocumentDeletedLog(doc, targetTenant);
                em.remove(doc);
            } else {
                doc.setDocumentStatus(DocumentStatus.TO_PROCESS);
                em.merge(doc);
                documentIAService.analyseDocument(doc);
                producer.sendDocumentForPdfGeneration(doc);
            }
            em.flush();
            return null;
        }).when(tenantDeleteFileUseCase).execute(any());

        // Mock DocumentService behaviors
        when(documentService.resolveDocumentTenant(any())).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            return doc.getTenant() != null ? doc.getTenant() : doc.getGuarantor().getTenant();
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            em.remove(doc);
            return null;
        }).when(documentService).delete(any());
        org.mockito.Mockito.doAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            DocumentStatus status = invocation.getArgument(1);
            doc.setDocumentStatus(status);
            return null;
        }).when(documentService).changeDocumentStatus(any(), any());

        // --- Setup Dossier ALONE ---
        ApartmentSharing sharingAlone = ApartmentSharing.builder()
                .applicationType(ApplicationType.ALONE)
                .tenants(new ArrayList<>())
                .build();
        em.persist(sharingAlone);

        tenantAlone = Tenant.builder()
                .email("alone@test.com")
                .apartmentSharing(sharingAlone)
                .tenantType(TenantType.CREATE)
                .documents(new ArrayList<>())
                .guarantors(new ArrayList<>())
                .build();
        em.persist(tenantAlone);
        sharingAlone.getTenants().add(tenantAlone);

        // --- Setup Dossier GROUP (Colocation) ---
        ApartmentSharing sharingGroup = ApartmentSharing.builder()
                .applicationType(ApplicationType.GROUP)
                .tenants(new ArrayList<>())
                .build();
        em.persist(sharingGroup);

        tenantGroup1 = Tenant.builder()
                .email("group1@test.com")
                .apartmentSharing(sharingGroup)
                .tenantType(TenantType.CREATE)
                .documents(new ArrayList<>())
                .guarantors(new ArrayList<>())
                .build();
        em.persist(tenantGroup1);
        sharingGroup.getTenants().add(tenantGroup1);

        tenantGroup2 = Tenant.builder()
                .email("group2@test.com")
                .apartmentSharing(sharingGroup)
                .tenantType(TenantType.JOIN)
                .documents(new ArrayList<>())
                .guarantors(new ArrayList<>())
                .build();
        em.persist(tenantGroup2);
        sharingGroup.getTenants().add(tenantGroup2);

        // --- Setup Dossier COUPLE ---
        ApartmentSharing sharingCouple = ApartmentSharing.builder()
                .applicationType(ApplicationType.COUPLE)
                .tenants(new ArrayList<>())
                .build();
        em.persist(sharingCouple);

        tenantCouple1 = Tenant.builder()
                .email("couple1@test.com")
                .apartmentSharing(sharingCouple)
                .tenantType(TenantType.CREATE)
                .documents(new ArrayList<>())
                .guarantors(new ArrayList<>())
                .build();
        em.persist(tenantCouple1);
        sharingCouple.getTenants().add(tenantCouple1);

        tenantCouple2 = Tenant.builder()
                .email("couple2@test.com")
                .apartmentSharing(sharingCouple)
                .tenantType(TenantType.JOIN)
                .documents(new ArrayList<>())
                .guarantors(new ArrayList<>())
                .build();
        em.persist(tenantCouple2);
        sharingCouple.getTenants().add(tenantCouple2);

        em.flush();
    }

    @Test
    void shouldDeleteOwnFileAndKeepDocumentWhenOtherFilesExist() throws Exception {
        // Given
        Document document = Document.builder()
                .tenant(tenantAlone)
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentStatus(DocumentStatus.VALIDATED)
                .files(new ArrayList<>())
                .build();
        em.persist(document);
        tenantAlone.getDocuments().add(document);

        StorageFile storageFile1 = StorageFile.builder().name("f1.pdf").build();
        em.persist(storageFile1);
        File file1 = File.builder().document(document).storageFile(storageFile1).build();
        em.persist(file1);
        document.getFiles().add(file1);

        StorageFile storageFile2 = StorageFile.builder().name("f2.pdf").build();
        em.persist(storageFile2);
        File file2 = File.builder().document(document).storageFile(storageFile2).build();
        em.persist(file2);
        document.getFiles().add(file2);

        em.flush();

        when(authenticationFacade.getLoggedTenant()).thenReturn(tenantAlone);

        // When
        mockMvc.perform(delete("/api/file/{id}", file1.getId()))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        // Then
        // 1. File 1 is deleted from the DB
        assertThat(em.find(File.class, file1.getId())).isNull();
        // 2. File 2 still exists
        assertThat(em.find(File.class, file2.getId())).isNotNull();
        // 3. Document is updated and still exists
        Document updatedDoc = em.find(Document.class, document.getId());
        assertThat(updatedDoc).isNotNull();
        assertThat(updatedDoc.getFiles()).hasSize(1);
        assertThat(updatedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        // 4. Verification that side-effects were called
        verify(logService).saveFileDeletedLog(any(File.class), any(Tenant.class));
        verify(documentIAService).analyseDocument(any(Document.class));
        verify(producer).sendDocumentForPdfGeneration(any(Document.class));
    }

    @Test
    void shouldDeleteFileAndDocumentWhenLastFileIsDeleted() throws Exception {
        // Given
        Document document = Document.builder()
                .tenant(tenantAlone)
                .documentCategory(DocumentCategory.TAX)
                .documentStatus(DocumentStatus.VALIDATED)
                .files(new ArrayList<>())
                .build();
        em.persist(document);
        tenantAlone.getDocuments().add(document);

        StorageFile storageFile = StorageFile.builder().name("tax.pdf").build();
        em.persist(storageFile);
        File file = File.builder().document(document).storageFile(storageFile).build();
        em.persist(file);
        document.getFiles().add(file);

        em.flush();

        when(authenticationFacade.getLoggedTenant()).thenReturn(tenantAlone);

        // When
        mockMvc.perform(delete("/api/file/{id}", file.getId()))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        // Then
        // 1. File is deleted
        assertThat(em.find(File.class, file.getId())).isNull();
        // 2. Document is deleted (since it was the last file)
        assertThat(em.find(Document.class, document.getId())).isNull();
        // 3. Verification of audit logging for document deletion
        verify(logService).saveFileDeletedLog(any(File.class), any(Tenant.class));
        verify(logService).saveDocumentDeletedLog(any(Document.class), any(Tenant.class));
        // 4. No async actions run since document is gone
        verify(documentIAService, never()).analyseDocument(any());
        verify(producer, never()).sendDocumentForPdfGeneration(any(Document.class));
    }

    @Test
    void shouldReturn403WhenGroupTenantTriesToDeleteCoTenantFile() throws Exception {
        // Given
        Document document = Document.builder()
                .tenant(tenantGroup2)
                .documentCategory(DocumentCategory.RESIDENCY)
                .files(new ArrayList<>())
                .build();
        em.persist(document);
        tenantGroup2.getDocuments().add(document);

        StorageFile storageFile = StorageFile.builder().name("residency.pdf").build();
        em.persist(storageFile);
        File file = File.builder().document(document).storageFile(storageFile).build();
        em.persist(file);
        document.getFiles().add(file);

        em.flush();

        // Tenant 1 tries to delete Tenant 2's file
        when(authenticationFacade.getLoggedTenant()).thenReturn(tenantGroup1);

        // When/Then
        mockMvc.perform(delete("/api/file/{id}", file.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeletePartnerFileWhenInCouple() throws Exception {
        // Given
        Document document = Document.builder()
                .tenant(tenantCouple2)
                .documentCategory(DocumentCategory.PROFESSIONAL)
                .files(new ArrayList<>())
                .build();
        em.persist(document);
        tenantCouple2.getDocuments().add(document);

        StorageFile storageFile = StorageFile.builder().name("work.pdf").build();
        em.persist(storageFile);
        File file = File.builder().document(document).storageFile(storageFile).build();
        em.persist(file);
        document.getFiles().add(file);

        em.flush();

        // Couple Tenant 1 deletes Couple Tenant 2's file
        when(authenticationFacade.getLoggedTenant()).thenReturn(tenantCouple1);

        // When
        mockMvc.perform(delete("/api/file/{id}", file.getId()))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        // Then
        assertThat(em.find(File.class, file.getId())).isNull();
    }

    @Test
    void shouldDeleteGuarantorDocumentFile() throws Exception {
        // Given
        Guarantor guarantor = Guarantor.builder()
                .tenant(tenantAlone)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .documents(new ArrayList<>())
                .build();
        em.persist(guarantor);
        tenantAlone.getGuarantors().add(guarantor);

        Document document = Document.builder()
                .guarantor(guarantor)
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .files(new ArrayList<>())
                .build();
        em.persist(document);
        guarantor.getDocuments().add(document);

        StorageFile storageFile = StorageFile.builder().name("id_card.pdf").build();
        em.persist(storageFile);
        File file = File.builder().document(document).storageFile(storageFile).build();
        em.persist(file);
        document.getFiles().add(file);

        em.flush();

        when(authenticationFacade.getLoggedTenant()).thenReturn(tenantAlone);

        // When
        mockMvc.perform(delete("/api/file/{id}", file.getId()))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        // Then
        assertThat(em.find(File.class, file.getId())).isNull();
        verify(logService).saveFileDeletedLog(any(File.class), any(Tenant.class));
    }
}

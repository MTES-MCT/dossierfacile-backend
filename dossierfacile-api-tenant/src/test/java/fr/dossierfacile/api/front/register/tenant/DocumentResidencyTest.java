package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.register.form.tenant.DocumentResidencyForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentResidencyTest {

    @Mock
    private DocumentHelperService documentHelperService;
    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private TenantStatusService tenantStatusService;
    @Mock
    private ApartmentSharingService apartmentSharingService;

    @InjectMocks
    private DocumentResidency documentResidency;

    private Tenant tenant;
    private ApartmentSharing apartmentSharing;

    @BeforeEach
    void setUp() {
        apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.ALONE)
                .build();
        tenant = Tenant.builder()
                .id(1L)
                .apartmentSharing(apartmentSharing)
                .documents(new ArrayList<>())
                .build();
        apartmentSharing.setTenants(List.of(tenant));

        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.save(any())).thenReturn(tenant);
    }

    @Test
    @DisplayName("Case 1: Creating a new residency document should set status to TO_PROCESS")
    void saveDocument_case1_createNewDocument_shouldSetToProcess() {
        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.RESIDENCY), eq(tenant)))
                .thenReturn(Optional.empty());

        DocumentResidencyForm form = new DocumentResidencyForm();
        form.setTypeDocumentResidency(DocumentSubCategory.TENANT);
        form.setDocuments(Collections.emptyList());

        var result = documentResidency.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isTrue();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getDocumentDeniedReasons()).isNull();
        assertThat(savedDoc.getNoDocument()).isFalse();

        verify(documentHelperService, never()).deleteFiles(any());
        verify(apartmentSharingService, times(1)).resetDossierPdfGenerated(tenant.getApartmentSharing());
    }

    @Test
    @DisplayName("Case 2: Transitioning from document with files (TENANT) to OTHER_RESIDENCY (noDocument=true) should delete files and set status to TO_PROCESS")
    void saveDocument_case2_transitionFromFilesToNoDocument_shouldDeleteFilesAndSetToProcess() {
        Document existingDoc = Document.builder()
                .id(100L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .tenant(tenant)
                .noDocument(false)
                .documentStatus(DocumentStatus.VALIDATED)
                .documentDeniedReasons(DocumentDeniedReasons.builder().build())
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.RESIDENCY), eq(tenant)))
                .thenReturn(Optional.of(existingDoc));

        DocumentResidencyForm form = new DocumentResidencyForm();
        form.setTypeDocumentResidency(DocumentSubCategory.OTHER_RESIDENCY);
        form.setCustomText("Hébergé à titre gratuit");
        form.setDocuments(Collections.emptyList());

        var result = documentResidency.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isFalse();
        assertThat(result.edited()).isTrue();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getDocumentDeniedReasons()).isNull();
        assertThat(savedDoc.getNoDocument()).isTrue();
        assertThat(savedDoc.getCustomText()).isEqualTo("Hébergé à titre gratuit");

        verify(documentHelperService, times(1)).deleteFiles(existingDoc);
        verify(apartmentSharingService, times(1)).resetDossierPdfGenerated(tenant.getApartmentSharing());
    }

    @Test
    @DisplayName("Case 3: Re-submitting OTHER_RESIDENCY with unchanged customText on an existing noDocument document should keep status unchanged and NOT reset dossier PDF")
    void saveDocument_case3_stayingNoDocumentUnchanged_shouldNotChangeStatusNorResetDossierPdf() {
        Document existingDoc = Document.builder()
                .id(100L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.OTHER_RESIDENCY)
                .tenant(tenant)
                .noDocument(true)
                .customText("Hébergé à titre gratuit")
                .documentStatus(DocumentStatus.VALIDATED)
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.RESIDENCY), eq(tenant)))
                .thenReturn(Optional.of(existingDoc));

        DocumentResidencyForm form = new DocumentResidencyForm();
        form.setTypeDocumentResidency(DocumentSubCategory.OTHER_RESIDENCY);
        form.setCustomText("Hébergé à titre gratuit");
        form.setDocuments(Collections.emptyList());

        var result = documentResidency.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isFalse();
        assertThat(result.edited()).isFalse();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.VALIDATED);
        assertThat(savedDoc.getNoDocument()).isTrue();

        verify(documentHelperService, never()).deleteFiles(any());
        verify(apartmentSharingService, never()).resetDossierPdfGenerated(any());
    }

    @Test
    @DisplayName("Case 3b: Staying in OTHER_RESIDENCY BUT changing customText SHOULD mark as edited and reset dossier PDF")
    void saveDocument_case3b_stayingNoDocument_whenCustomTextChanged_shouldMarkEditedAndResetDossierPdf() {
        Document existingDoc = Document.builder()
                .id(100L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.OTHER_RESIDENCY)
                .tenant(tenant)
                .noDocument(true)
                .customText("Hébergé à titre gratuit")
                .documentStatus(DocumentStatus.VALIDATED)
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.RESIDENCY), eq(tenant)))
                .thenReturn(Optional.of(existingDoc));

        DocumentResidencyForm form = new DocumentResidencyForm();
        form.setTypeDocumentResidency(DocumentSubCategory.OTHER_RESIDENCY);
        form.setCustomText("Hébergé chez les parents");
        form.setDocuments(Collections.emptyList());

        var result = documentResidency.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isFalse();
        assertThat(result.edited()).isTrue();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getNoDocument()).isTrue();
        assertThat(savedDoc.getCustomText()).isEqualTo("Hébergé chez les parents");

        verify(apartmentSharingService, times(1)).resetDossierPdfGenerated(tenant.getApartmentSharing());
    }

    @Test
    @DisplayName("Case 4: Switching from OTHER_RESIDENCY to TENANT (with files) should set status to TO_PROCESS")
    void saveDocument_case4_switchingFromNoDocumentToFiles_shouldSetToProcess() {
        Document existingDoc = Document.builder()
                .id(100L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.OTHER_RESIDENCY)
                .tenant(tenant)
                .noDocument(true)
                .customText("Hébergé à titre gratuit")
                .documentStatus(DocumentStatus.VALIDATED)
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.RESIDENCY), eq(tenant)))
                .thenReturn(Optional.of(existingDoc));

        DocumentResidencyForm form = new DocumentResidencyForm();
        form.setTypeDocumentResidency(DocumentSubCategory.TENANT);
        form.setDocuments(Collections.emptyList());

        var result = documentResidency.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isFalse();
        assertThat(result.edited()).isTrue();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getNoDocument()).isFalse();
        assertThat(savedDoc.getCustomText()).isNull();

        verify(documentHelperService, never()).deleteFiles(any());
        verify(apartmentSharingService, times(1)).resetDossierPdfGenerated(tenant.getApartmentSharing());
    }
}

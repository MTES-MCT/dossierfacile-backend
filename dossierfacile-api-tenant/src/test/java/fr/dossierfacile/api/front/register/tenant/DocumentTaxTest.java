package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
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
class DocumentTaxTest {

    @Mock
    private DocumentHelperService documentHelperService;
    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentService documentService;
    @Mock
    private TenantStatusService tenantStatusService;
    @Mock
    private ApartmentSharingService apartmentSharingService;

    @InjectMocks
    private DocumentTax documentTax;

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
    @DisplayName("Case 1: Creating a new tax document should set status to TO_PROCESS and reset associated categories")
    void saveDocument_case1_createNewDocument_shouldSetToProcessAndResetCategories() {
        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.TAX), eq(tenant)))
                .thenReturn(Optional.empty());

        DocumentTaxForm form = new DocumentTaxForm();
        form.setTypeDocumentTax(DocumentSubCategory.MY_NAME);
        form.setNoDocument(false);
        form.setDocuments(Collections.emptyList());

        var result = documentTax.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isTrue();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getDocumentDeniedReasons()).isNull();
        assertThat(savedDoc.getNoDocument()).isFalse();

        verify(documentService, times(1))
                .resetValidatedOrInProgressDocumentsAccordingCategories(eq(tenant.getDocuments()), anyList());
        verify(documentHelperService, never()).deleteFiles(any());
    }

    @Test
    @DisplayName("Case 2: Transitioning from document with files (noDocument=false) to no document (noDocument=true) should delete files, set status to TO_PROCESS and reset associated categories")
    void saveDocument_case2_transitionFromFilesToNoDocument_shouldDeleteFilesSetToProcessAndResetCategories() {
        Document existingDoc = Document.builder()
                .id(100L)
                .documentCategory(DocumentCategory.TAX)
                .tenant(tenant)
                .noDocument(false)
                .documentStatus(DocumentStatus.VALIDATED)
                .documentDeniedReasons(DocumentDeniedReasons.builder().build())
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.TAX), eq(tenant)))
                .thenReturn(Optional.of(existingDoc));

        DocumentTaxForm form = new DocumentTaxForm();
        form.setTypeDocumentTax(DocumentSubCategory.MY_NAME);
        form.setNoDocument(true);
        form.setDocuments(Collections.emptyList());

        var result = documentTax.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isFalse();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getDocumentDeniedReasons()).isNull();
        assertThat(savedDoc.getNoDocument()).isTrue();

        verify(documentHelperService, times(1)).deleteFiles(existingDoc);
        verify(documentService, times(1))
                .resetValidatedOrInProgressDocumentsAccordingCategories(eq(tenant.getDocuments()), anyList());
    }

    @Test
    @DisplayName("Case 3: Re-submitting noDocument=true on an existing noDocument=true document should keep status unchanged and NOT reset associated categories")
    void saveDocument_case3_stayingNoDocument_shouldNotChangeStatusNorResetCategories() {
        Document existingDoc = Document.builder()
                .id(100L)
                .documentCategory(DocumentCategory.TAX)
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .tenant(tenant)
                .noDocument(true)
                .documentStatus(DocumentStatus.VALIDATED)
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.TAX), eq(tenant)))
                .thenReturn(Optional.of(existingDoc));

        DocumentTaxForm form = new DocumentTaxForm();
        form.setTypeDocumentTax(DocumentSubCategory.MY_NAME);
        form.setNoDocument(true);
        form.setDocuments(Collections.emptyList());

        var result = documentTax.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isFalse();
        assertThat(result.edited()).isFalse();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.VALIDATED);
        assertThat(savedDoc.getNoDocument()).isTrue();

        verify(documentHelperService, never()).deleteFiles(any());
        verify(documentService, never())
                .resetValidatedOrInProgressDocumentsAccordingCategories(anyList(), anyList());
        verify(apartmentSharingService, never()).resetDossierPdfGenerated(any());
    }

    @Test
    @DisplayName("Case 3b: Staying in noDocument=true BUT changing subcategory/step SHOULD mark as edited and regenerate PDF")
    void saveDocument_case3b_stayingNoDocument_whenSubCategoryOrCategoryStepChanges_shouldMarkEditedAndRegeneratePdf() {
        Document existingDoc = Document.builder()
                .id(100L)
                .documentCategory(DocumentCategory.TAX)
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .tenant(tenant)
                .noDocument(true)
                .documentStatus(DocumentStatus.VALIDATED)
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.TAX), eq(tenant)))
                .thenReturn(Optional.of(existingDoc));

        DocumentTaxForm form = new DocumentTaxForm();
        form.setTypeDocumentTax(DocumentSubCategory.OTHER_TAX);
        form.setNoDocument(true);
        form.setCustomText("Nouvelle explication");
        form.setDocuments(Collections.emptyList());

        var result = documentTax.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isFalse();
        assertThat(result.edited()).isTrue();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getNoDocument()).isTrue();
        assertThat(savedDoc.getDocumentSubCategory()).isEqualTo(DocumentSubCategory.OTHER_TAX);

        verify(documentService, times(1))
                .resetValidatedOrInProgressDocumentsAccordingCategories(eq(tenant.getDocuments()), anyList());
        verify(apartmentSharingService, times(1)).resetDossierPdfGenerated(any());
    }

    @Test
    @DisplayName("Case 4: Adding files to an existing document (noDocument=false) should set status to TO_PROCESS and reset associated categories")
    void saveDocument_case4_existingDocumentAddingFiles_shouldSetToProcessAndResetCategories() {
        Document existingDoc = Document.builder()
                .id(100L)
                .documentCategory(DocumentCategory.TAX)
                .tenant(tenant)
                .noDocument(true)
                .documentStatus(DocumentStatus.VALIDATED)
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.TAX), eq(tenant)))
                .thenReturn(Optional.of(existingDoc));

        DocumentTaxForm form = new DocumentTaxForm();
        form.setTypeDocumentTax(DocumentSubCategory.MY_NAME);
        form.setNoDocument(false);
        form.setDocuments(Collections.emptyList());

        var result = documentTax.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isFalse();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getNoDocument()).isFalse();

        verify(documentHelperService, never()).deleteFiles(any());
        verify(documentService, times(1))
                .resetValidatedOrInProgressDocumentsAccordingCategories(eq(tenant.getDocuments()), anyList());
    }
}

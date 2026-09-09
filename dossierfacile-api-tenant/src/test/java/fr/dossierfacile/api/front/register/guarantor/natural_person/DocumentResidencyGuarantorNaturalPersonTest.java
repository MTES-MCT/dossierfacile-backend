package fr.dossierfacile.api.front.register.guarantor.natural_person;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentResidencyGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.FileUploadPreprocessor;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentResidencyGuarantorNaturalPersonTest {

    private static final Long GUARANTOR_ID = 10L;

    @Mock
    private DocumentHelperService documentHelperService;
    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private GuarantorRepository guarantorRepository;
    @Mock
    private TenantStatusService tenantStatusService;
    @Mock
    private ApartmentSharingService apartmentSharingService;
    @Mock
    private FileUploadPreprocessor fileUploadPreprocessor;
    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentResidencyGuarantorNaturalPerson documentResidencyGuarantor;

    private Tenant tenant;
    private Guarantor guarantor;

    @BeforeEach
    void setUp() {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.ALONE)
                .build();
        tenant = Tenant.builder()
                .id(1L)
                .apartmentSharing(apartmentSharing)
                .documents(new ArrayList<>())
                .build();
        apartmentSharing.setTenants(List.of(tenant));

        guarantor = Guarantor.builder()
                .id(GUARANTOR_ID)
                .tenant(tenant)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .documents(new ArrayList<>())
                .build();

        when(guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.NATURAL_PERSON, GUARANTOR_ID))
                .thenReturn(Optional.of(guarantor));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.save(any())).thenReturn(tenant);

        ReflectionTestUtils.setField(documentResidencyGuarantor, "fileUploadPreprocessor", fileUploadPreprocessor);
        ReflectionTestUtils.setField(documentResidencyGuarantor, "documentService", documentService);
    }

    @Test
    @DisplayName("Case 1: Creating a new guarantor residency document should set status to TO_PROCESS")
    void saveDocument_case1_createNewDocument_shouldSetToProcess() {
        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(DocumentCategory.RESIDENCY, guarantor))
                .thenReturn(Optional.empty());

        DocumentResidencyGuarantorNaturalPersonForm form = new DocumentResidencyGuarantorNaturalPersonForm();
        form.setGuarantorId(GUARANTOR_ID);
        form.setTypeDocumentResidency(DocumentSubCategory.TENANT);
        form.setDocuments(Collections.emptyList());

        var result = documentResidencyGuarantor.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isTrue();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getDocumentDeniedReasons()).isNull();
        assertThat(savedDoc.getNoDocument()).isFalse();

        verify(documentHelperService, never()).deleteFiles(any());
        verify(apartmentSharingService, times(1)).resetDossierPdfGenerated(tenant.getApartmentSharing());
    }

    @Test
    @DisplayName("Case 2: Transitioning guarantor doc from TENANT to OTHER_RESIDENCY (noDocument=true) should delete files and set status to TO_PROCESS")
    void saveDocument_case2_transitionFromFilesToNoDocument_shouldDeleteFilesAndSetToProcess() {
        Document existingDoc = Document.builder()
                .id(200L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .guarantor(guarantor)
                .noDocument(false)
                .documentStatus(DocumentStatus.VALIDATED)
                .documentDeniedReasons(DocumentDeniedReasons.builder().build())
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(DocumentCategory.RESIDENCY, guarantor))
                .thenReturn(Optional.of(existingDoc));

        DocumentResidencyGuarantorNaturalPersonForm form = new DocumentResidencyGuarantorNaturalPersonForm();
        form.setGuarantorId(GUARANTOR_ID);
        form.setTypeDocumentResidency(DocumentSubCategory.OTHER_RESIDENCY);
        form.setCustomText("Hébergé à titre gratuit");
        form.setDocuments(Collections.emptyList());

        var result = documentResidencyGuarantor.saveDocument(tenant, form);

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
    @DisplayName("Case 2b: Guarantor submitting OTHER_RESIDENCY with files attached should save files and set noDocument=false")
    void saveDocument_case2b_otherResidencyWithFiles_shouldSaveFilesAndSetNoDocumentFalse() {
        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(DocumentCategory.RESIDENCY, guarantor))
                .thenReturn(Optional.empty());

        MockMultipartFile mockFile = new MockMultipartFile("documents", "attestation.pdf", "application/pdf", "content".getBytes());
        try {
            when(fileUploadPreprocessor.prepareValidatedFiles(any())).thenReturn(List.of(new fr.dossierfacile.common.model.ValidatedFile(mockFile, "application/pdf")));
        } catch (IOException ignored) {
            // Empty catch for this test
        }

        DocumentResidencyGuarantorNaturalPersonForm form = new DocumentResidencyGuarantorNaturalPersonForm();
        form.setGuarantorId(GUARANTOR_ID);
        form.setTypeDocumentResidency(DocumentSubCategory.OTHER_RESIDENCY);
        form.setCustomText("Attestation d'hébergement");
        form.setDocuments(List.of(mockFile));

        var result = documentResidencyGuarantor.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isTrue();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getNoDocument()).isFalse();
        assertThat(savedDoc.getCustomText()).isEqualTo("Attestation d'hébergement");

        verify(documentHelperService, never()).deleteFiles(any());
        verify(apartmentSharingService, times(1)).resetDossierPdfGenerated(tenant.getApartmentSharing());
    }

    @Test
    @DisplayName("Case 3: Re-submitting OTHER_RESIDENCY with unchanged customText on an existing noDocument guarantor doc should keep status unchanged and NOT reset dossier PDF")
    void saveDocument_case3_stayingNoDocumentUnchanged_shouldNotChangeStatusNorResetDossierPdf() {
        Document existingDoc = Document.builder()
                .id(200L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.OTHER_RESIDENCY)
                .guarantor(guarantor)
                .noDocument(true)
                .customText("Hébergé à titre gratuit")
                .documentStatus(DocumentStatus.VALIDATED)
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(DocumentCategory.RESIDENCY, guarantor))
                .thenReturn(Optional.of(existingDoc));

        DocumentResidencyGuarantorNaturalPersonForm form = new DocumentResidencyGuarantorNaturalPersonForm();
        form.setGuarantorId(GUARANTOR_ID);
        form.setTypeDocumentResidency(DocumentSubCategory.OTHER_RESIDENCY);
        form.setCustomText("Hébergé à titre gratuit");
        form.setDocuments(Collections.emptyList());

        var result = documentResidencyGuarantor.saveDocument(tenant, form);

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
                .id(200L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.OTHER_RESIDENCY)
                .guarantor(guarantor)
                .noDocument(true)
                .customText("Hébergé à titre gratuit")
                .documentStatus(DocumentStatus.VALIDATED)
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(DocumentCategory.RESIDENCY, guarantor))
                .thenReturn(Optional.of(existingDoc));

        DocumentResidencyGuarantorNaturalPersonForm form = new DocumentResidencyGuarantorNaturalPersonForm();
        form.setGuarantorId(GUARANTOR_ID);
        form.setTypeDocumentResidency(DocumentSubCategory.OTHER_RESIDENCY);
        form.setCustomText("Propriétaire");
        form.setDocuments(Collections.emptyList());

        var result = documentResidencyGuarantor.saveDocument(tenant, form);

        Document savedDoc = result.document();
        assertThat(result.created()).isFalse();
        assertThat(result.edited()).isTrue();
        assertThat(savedDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(savedDoc.getNoDocument()).isTrue();
        assertThat(savedDoc.getCustomText()).isEqualTo("Propriétaire");

        verify(apartmentSharingService, times(1)).resetDossierPdfGenerated(tenant.getApartmentSharing());
    }

    @Test
    @DisplayName("Case 4: Switching guarantor doc from OTHER_RESIDENCY to TENANT (with files) should set status to TO_PROCESS")
    void saveDocument_case4_switchingFromNoDocumentToFiles_shouldSetToProcess() {
        Document existingDoc = Document.builder()
                .id(200L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.OTHER_RESIDENCY)
                .guarantor(guarantor)
                .noDocument(true)
                .customText("Hébergé à titre gratuit")
                .documentStatus(DocumentStatus.VALIDATED)
                .build();

        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(DocumentCategory.RESIDENCY, guarantor))
                .thenReturn(Optional.of(existingDoc));

        DocumentResidencyGuarantorNaturalPersonForm form = new DocumentResidencyGuarantorNaturalPersonForm();
        form.setGuarantorId(GUARANTOR_ID);
        form.setTypeDocumentResidency(DocumentSubCategory.TENANT);
        form.setDocuments(Collections.emptyList());

        var result = documentResidencyGuarantor.saveDocument(tenant, form);

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

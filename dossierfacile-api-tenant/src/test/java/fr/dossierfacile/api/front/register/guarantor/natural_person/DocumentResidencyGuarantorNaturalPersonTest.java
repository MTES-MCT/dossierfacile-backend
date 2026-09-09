package fr.dossierfacile.api.front.register.guarantor.natural_person;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentResidencyGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
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

        when(guarantorRepository.findByTenantAndTypeGuarantorAndId(eq(tenant), eq(TypeGuarantor.NATURAL_PERSON), eq(GUARANTOR_ID)))
                .thenReturn(Optional.of(guarantor));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.save(any())).thenReturn(tenant);
    }

    @Test
    @DisplayName("Case 1: Creating a new guarantor residency document should set status to TO_PROCESS")
    void saveDocument_case1_createNewDocument_shouldSetToProcess() {
        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(eq(DocumentCategory.RESIDENCY), eq(guarantor)))
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

        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(eq(DocumentCategory.RESIDENCY), eq(guarantor)))
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

        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(eq(DocumentCategory.RESIDENCY), eq(guarantor)))
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

        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(eq(DocumentCategory.RESIDENCY), eq(guarantor)))
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

        when(documentRepository.findFirstByDocumentCategoryAndGuarantor(eq(DocumentCategory.RESIDENCY), eq(guarantor)))
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

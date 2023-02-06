package fr.dossierfacile.api.front.register.guarantor.legal_person;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.register.AbstractDocumentSaveStep;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.legal_person.DocumentIdentificationGuarantorLegalPersonForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DocumentIdentificationGuarantorLegalPerson
        extends AbstractDocumentSaveStep<DocumentIdentificationGuarantorLegalPersonForm>
        implements SaveStep<DocumentIdentificationGuarantorLegalPersonForm> {

    private final TenantCommonRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final GuarantorRepository guarantorRepository;
    private final DocumentService documentService;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    protected Document saveDocument(Tenant tenant, DocumentIdentificationGuarantorLegalPersonForm documentIdentificationGuarantorLegalPersonForm) {
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.LEGAL_PERSON, documentIdentificationGuarantorLegalPersonForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(documentIdentificationGuarantorLegalPersonForm.getGuarantorId()));
        guarantor.setLegalPersonName(documentIdentificationGuarantorLegalPersonForm.getLegalPersonName());
        guarantor.setTenant(tenant);
        guarantorRepository.save(guarantor);

        DocumentSubCategory documentSubCategory = DocumentSubCategory.LEGAL_PERSON;
        Document document = documentRepository.findFirstByDocumentCategoryAndGuarantor(DocumentCategory.IDENTIFICATION_LEGAL_PERSON, guarantor)
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.IDENTIFICATION_LEGAL_PERSON)
                        .guarantor(guarantor)
                        .build());
        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
        document.setDocumentDeniedReasons(null);
        document.setDocumentSubCategory(documentSubCategory);
        documentRepository.save(document);

        documentIdentificationGuarantorLegalPersonForm.getDocuments().stream()
                .filter(f -> !f.isEmpty())
                .forEach(multipartFile -> documentService.addFile(multipartFile, document));

        documentService.initializeFieldsToProcessPdfGeneration(document);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.IDENTIFICATION_LEGAL_PERSON);
        tenantStatusService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenantRepository.save(tenant);
        return document;
    }
}

package fr.dossierfacile.api.front.register.guarantor.natural_person;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.register.AbstractDocumentFinancialSaveStep;
import fr.dossierfacile.api.front.register.DocumentSaveResult;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentFinancialGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DocumentFinancialGuarantorNaturalPerson extends AbstractDocumentFinancialSaveStep<DocumentFinancialGuarantorNaturalPersonForm> implements SaveStep<DocumentFinancialGuarantorNaturalPersonForm> {

    private final GuarantorRepository guarantorRepository;

    public DocumentFinancialGuarantorNaturalPerson(
            DocumentHelperService documentHelperService,
            TenantCommonRepository tenantRepository,
            DocumentRepository documentRepository,
            GuarantorRepository guarantorRepository,
            DocumentService documentService,
            TenantStatusService tenantStatusService,
            ApartmentSharingService apartmentSharingService) {
        super(documentHelperService, tenantRepository, documentRepository, documentService, tenantStatusService, apartmentSharingService);
        this.guarantorRepository = guarantorRepository;
    }

    @Override
    protected DocumentSaveResult saveDocument(Tenant tenant, DocumentFinancialGuarantorNaturalPersonForm documentFinancialGuarantorNaturalPersonForm) {
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.NATURAL_PERSON, documentFinancialGuarantorNaturalPersonForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(documentFinancialGuarantorNaturalPersonForm.getGuarantorId()));

        Document document = documentRepository.findByDocumentCategoryAndGuarantorAndId(DocumentCategory.FINANCIAL, guarantor, documentFinancialGuarantorNaturalPersonForm.getDocumentId())
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.FINANCIAL)
                        .documentCategoryStep(documentFinancialGuarantorNaturalPersonForm.getCategoryStep())
                        .guarantor(guarantor)
                        .build());

        return processFinancialDocument(tenant, document, documentFinancialGuarantorNaturalPersonForm, guarantor.getDocuments());
    }
}

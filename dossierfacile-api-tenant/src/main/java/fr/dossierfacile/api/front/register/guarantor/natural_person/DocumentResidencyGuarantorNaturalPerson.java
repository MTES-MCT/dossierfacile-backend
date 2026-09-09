package fr.dossierfacile.api.front.register.guarantor.natural_person;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.register.AbstractDocumentResidencySaveStep;
import fr.dossierfacile.api.front.register.DocumentSaveResult;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentResidencyGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
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
public class DocumentResidencyGuarantorNaturalPerson
        extends AbstractDocumentResidencySaveStep<DocumentResidencyGuarantorNaturalPersonForm>
        implements SaveStep<DocumentResidencyGuarantorNaturalPersonForm> {

    private final GuarantorRepository guarantorRepository;

    public DocumentResidencyGuarantorNaturalPerson(
            DocumentHelperService documentHelperService,
            TenantCommonRepository tenantRepository,
            DocumentRepository documentRepository,
            GuarantorRepository guarantorRepository,
            TenantStatusService tenantStatusService,
            ApartmentSharingService apartmentSharingService) {
        super(documentHelperService, tenantRepository, documentRepository, tenantStatusService, apartmentSharingService);
        this.guarantorRepository = guarantorRepository;
    }

    @Override
    protected DocumentSaveResult saveDocument(Tenant tenant, DocumentResidencyGuarantorNaturalPersonForm documentResidencyGuarantorNaturalPersonForm) {
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.NATURAL_PERSON, documentResidencyGuarantorNaturalPersonForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(documentResidencyGuarantorNaturalPersonForm.getGuarantorId()));

        Document document = documentRepository.findFirstByDocumentCategoryAndGuarantor(DocumentCategory.RESIDENCY, guarantor)
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.RESIDENCY)
                        .guarantor(guarantor)
                        .build());

        return processResidencyDocument(tenant, document, documentResidencyGuarantorNaturalPersonForm);
    }
}

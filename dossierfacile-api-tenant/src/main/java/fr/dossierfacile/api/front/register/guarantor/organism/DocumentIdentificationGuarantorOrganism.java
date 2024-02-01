package fr.dossierfacile.api.front.register.guarantor.organism;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.register.AbstractDocumentSaveStep;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.organism.DocumentIdentificationGuarantorOrganismForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static fr.dossierfacile.common.enums.DocumentCategory.*;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_GUARANTEE;

@Service
@Deprecated
@RequiredArgsConstructor
public class DocumentIdentificationGuarantorOrganism
        extends AbstractDocumentSaveStep<DocumentIdentificationGuarantorOrganismForm>
        implements SaveStep<DocumentIdentificationGuarantorOrganismForm> {

    private final TenantCommonRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final GuarantorRepository guarantorRepository;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    protected Document saveDocument(Tenant tenant, DocumentIdentificationGuarantorOrganismForm documentIdentificationGuarantorOrganismForm) {
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.ORGANISM, documentIdentificationGuarantorOrganismForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(documentIdentificationGuarantorOrganismForm.getGuarantorId()));

        Document document = documentRepository.findFirstByDocumentCategoryAndGuarantor(GUARANTEE_PROVIDER_CERTIFICATE, guarantor)
                .orElse(Document.builder()
                        .documentCategory(GUARANTEE_PROVIDER_CERTIFICATE)
                        .guarantor(guarantor)
                        .build());
        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
        document.setDocumentDeniedReasons(null);
        document.setDocumentSubCategory(OTHER_GUARANTEE);
        documentRepository.save(document);

        saveFiles(documentIdentificationGuarantorOrganismForm, document);

        tenant.lastUpdateDateProfile(LocalDateTime.now(), GUARANTEE_PROVIDER_CERTIFICATE);
        tenantStatusService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenantRepository.save(tenant);
        return document;
    }
}

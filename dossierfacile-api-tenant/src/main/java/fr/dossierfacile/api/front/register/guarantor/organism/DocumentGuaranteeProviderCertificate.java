package fr.dossierfacile.api.front.register.guarantor.organism;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.register.AbstractDocumentSaveStep;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.organism.DocumentGuaranteeProviderCertificateForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static fr.dossierfacile.common.enums.DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE;

@Service
@RequiredArgsConstructor
public class DocumentGuaranteeProviderCertificate
        extends AbstractDocumentSaveStep<DocumentGuaranteeProviderCertificateForm>
        implements SaveStep<DocumentGuaranteeProviderCertificateForm> {

    private final TenantCommonRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final GuarantorRepository guarantorRepository;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    protected Document saveDocument(Tenant tenant, DocumentGuaranteeProviderCertificateForm form) {
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.ORGANISM, form.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(form.getGuarantorId()));

        Document document = documentRepository.findFirstByDocumentCategoryAndGuarantor(GUARANTEE_PROVIDER_CERTIFICATE, guarantor)
                .orElse(Document.builder()
                        .documentCategory(GUARANTEE_PROVIDER_CERTIFICATE)
                        .guarantor(guarantor)
                        .build());
        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
        document.setDocumentDeniedReasons(null);
        document.setDocumentSubCategory(form.getTypeDocumentCertificate());
        documentRepository.save(document);

        saveFiles(form, document);

        tenant.lastUpdateDateProfile(LocalDateTime.now(), GUARANTEE_PROVIDER_CERTIFICATE);
        tenantStatusService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenantRepository.save(tenant);
        return document;
    }
}

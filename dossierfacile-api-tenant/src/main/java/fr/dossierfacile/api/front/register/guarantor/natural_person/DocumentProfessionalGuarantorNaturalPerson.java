package fr.dossierfacile.api.front.register.guarantor.natural_person;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentProfessionalGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentPdfGenerationLog;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.DocumentPdfGenerationLogRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class DocumentProfessionalGuarantorNaturalPerson implements SaveStep<DocumentProfessionalGuarantorNaturalPersonForm> {

    private final DocumentHelperService documentHelperService;
    private final TenantCommonRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final TenantMapper tenantMapper;
    private final GuarantorRepository guarantorRepository;
    private final DocumentService documentService;
    private final TenantService tenantService;
    private final Producer producer;
    private final ApartmentSharingService apartmentSharingService;
    private final DocumentPdfGenerationLogRepository documentPdfGenerationLogRepository;

    @Override
    public TenantModel saveStep(Tenant tenant, DocumentProfessionalGuarantorNaturalPersonForm documentProfessionalGuarantorNaturalPersonForm) {
        Document document = saveDocument(tenant, documentProfessionalGuarantorNaturalPersonForm);
        producer.generatePdf(document.getId(),
                documentPdfGenerationLogRepository.save(DocumentPdfGenerationLog.builder()
                        .documentId(document.getId())
                        .build()).getId());
        return tenantMapper.toTenantModel(document.getGuarantor().getTenant());
    }

    @Transactional
    Document saveDocument(Tenant tenant, DocumentProfessionalGuarantorNaturalPersonForm documentProfessionalGuarantorNaturalPersonForm) {
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.NATURAL_PERSON, documentProfessionalGuarantorNaturalPersonForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(documentProfessionalGuarantorNaturalPersonForm.getGuarantorId()));

        DocumentSubCategory documentSubCategory = documentProfessionalGuarantorNaturalPersonForm.getTypeDocumentProfessional();
        Document document = documentRepository.findFirstByDocumentCategoryAndGuarantor(DocumentCategory.PROFESSIONAL, guarantor)
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.PROFESSIONAL)
                        .guarantor(guarantor)
                        .build());
        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
        document.setDocumentDeniedReasons(null);
        document.setDocumentSubCategory(documentSubCategory);
        documentRepository.save(document);

        documentProfessionalGuarantorNaturalPersonForm.getDocuments().stream()
                .filter(f -> !f.isEmpty())
                .forEach(multipartFile -> documentHelperService.addFile(multipartFile, document));

        documentService.initializeFieldsToProcessPdfGeneration(document);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.PROFESSIONAL);
        documentService.resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(guarantor.getDocuments(), List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX));
        tenantService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenantRepository.save(tenant);
        return document;
    }
}

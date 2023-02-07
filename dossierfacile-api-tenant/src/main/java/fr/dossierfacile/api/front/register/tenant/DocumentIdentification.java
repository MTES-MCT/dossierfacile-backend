package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.register.AbstractDocumentSaveStep;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.DocumentIdentificationForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.FileService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class DocumentIdentification extends AbstractDocumentSaveStep<DocumentIdentificationForm> implements SaveStep<DocumentIdentificationForm> {
    @Autowired
    private TenantCommonRepository tenantRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private TenantStatusService tenantStatusService;
    @Autowired
    private ApartmentSharingService apartmentSharingService;
    @Autowired
    private FileService fileService;


    protected Document saveDocument(Tenant tenant, DocumentIdentificationForm documentIdentificationForm) {
        DocumentSubCategory documentSubCategory = documentIdentificationForm.getTypeDocumentIdentification();
        Document document = documentRepository.findFirstByDocumentCategoryAndTenant(DocumentCategory.IDENTIFICATION, tenant)
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.IDENTIFICATION)
                        .tenant(tenant)
                        .build());
        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
        document.setDocumentDeniedReasons(null);
        document.setDocumentSubCategory(documentSubCategory);
        documentRepository.save(document);

        documentIdentificationForm.getDocuments().stream()
                .filter(f -> !f.isEmpty())
                .forEach(multipartFile -> documentService.addFile(multipartFile, document)
                );

        documentService.initializeFieldsToProcessPdfGeneration(document);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.IDENTIFICATION);
        tenantStatusService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenantRepository.save(tenant);
        return document;
    }
}

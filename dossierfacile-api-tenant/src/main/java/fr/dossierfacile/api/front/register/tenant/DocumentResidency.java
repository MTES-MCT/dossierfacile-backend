package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.DocumentResidencyForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.util.Utility;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.service.interfaces.OvhService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DocumentResidency implements SaveStep<DocumentResidencyForm> {

    private final OvhService ovhService;
    private final TenantRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final TenantMapper tenantMapper;
    private final FileRepository fileRepository;
    private final DocumentService documentService;
    private final TenantService tenantService;
    private final Producer producer;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    public TenantModel saveStep(Tenant tenant, DocumentResidencyForm documentResidencyForm) {
        Document document = saveDocument(tenant, documentResidencyForm);
        producer.generatePdf(document.getId());
        return tenantMapper.toTenantModel(document.getTenant());
    }

    @Transactional
    Document saveDocument(Tenant tenant, DocumentResidencyForm documentResidencyForm) {
        DocumentSubCategory documentSubCategory = documentResidencyForm.getTypeDocumentResidency();
        Document document = documentRepository.findFirstByDocumentCategoryAndTenant(DocumentCategory.RESIDENCY, tenant)
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.RESIDENCY)
                        .tenant(tenant)
                        .build());
        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
        document.setDocumentSubCategory(documentSubCategory);
        documentRepository.save(document);

        List<MultipartFile> multipartFiles = documentResidencyForm.getDocuments().stream().filter(f -> !f.isEmpty()).collect(Collectors.toList());
        for (MultipartFile multipartFile : multipartFiles) {
            String originalName = multipartFile.getOriginalFilename();
            long size = multipartFile.getSize();
            String name = ovhService.uploadFile(multipartFile);
            File file = File.builder()
                    .path(name)
                    .document(document)
                    .originalName(originalName)
                    .size(size)
                    .numberOfPages(Utility.countNumberOfPagesOfPdfDocument(multipartFile))
                    .build();
            fileRepository.save(file);
        }
        documentService.initializeFieldsToProcessPdfGeneration(document);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.RESIDENCY);
        documentService.resetValidatedDocumentsStatusToToProcess(tenant);
        tenantService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenantRepository.save(tenant);
        return document;
    }
}

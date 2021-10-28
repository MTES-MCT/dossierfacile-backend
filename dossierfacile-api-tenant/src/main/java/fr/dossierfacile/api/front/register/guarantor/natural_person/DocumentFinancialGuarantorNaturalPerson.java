package fr.dossierfacile.api.front.register.guarantor.natural_person;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentFinancialGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.util.Utility;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.service.interfaces.OvhService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class DocumentFinancialGuarantorNaturalPerson implements SaveStep<DocumentFinancialGuarantorNaturalPersonForm> {

    private final OvhService ovhService;
    private final TenantRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final TenantMapper tenantMapper;
    private final GuarantorRepository guarantorRepository;
    private final FileRepository fileRepository;
    private final DocumentService documentService;
    private final TenantService tenantService;
    private final Producer producer;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    public TenantModel saveStep(Tenant tenant, DocumentFinancialGuarantorNaturalPersonForm documentFinancialGuarantorNaturalPersonForm) {
        Document document = saveDocument(tenant, documentFinancialGuarantorNaturalPersonForm);
        producer.generatePdf(document.getId());
        return tenantMapper.toTenantModel(document.getGuarantor().getTenant());
    }

    @Transactional
    Document saveDocument(Tenant tenant, DocumentFinancialGuarantorNaturalPersonForm documentFinancialGuarantorNaturalPersonForm) {
        documentService.resetValidatedDocumentsStatusToToProcess(tenant);
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.NATURAL_PERSON, documentFinancialGuarantorNaturalPersonForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(documentFinancialGuarantorNaturalPersonForm.getGuarantorId()));

        DocumentSubCategory documentSubCategory = documentFinancialGuarantorNaturalPersonForm.getTypeDocumentFinancial();
        Document document = documentRepository.findByDocumentCategoryAndGuarantorAndId(DocumentCategory.FINANCIAL, guarantor, documentFinancialGuarantorNaturalPersonForm.getDocumentId())
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.FINANCIAL)
                        .guarantor(guarantor)
                        .build());
        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
        document.setDocumentSubCategory(documentSubCategory);
        document.setMonthlySum(documentFinancialGuarantorNaturalPersonForm.getMonthlySum());
        if (document.getNoDocument() != null && !document.getNoDocument() && documentFinancialGuarantorNaturalPersonForm.getNoDocument()) {
            deleteFilesIfExistedBefore(document);
        }
        document.setNoDocument(documentFinancialGuarantorNaturalPersonForm.getNoDocument());
        documentRepository.save(document);

        if (Boolean.FALSE.equals(documentFinancialGuarantorNaturalPersonForm.getNoDocument())) {
            if (documentFinancialGuarantorNaturalPersonForm.getDocuments().size() > 0) {
                List<MultipartFile> multipartFiles = documentFinancialGuarantorNaturalPersonForm.getDocuments().stream().filter(f -> !f.isEmpty()).collect(Collectors.toList());
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
                document.setCustomText(null);
            } else {
                log.info("Refreshing info in [FINANCIAL] document with ID [" + documentFinancialGuarantorNaturalPersonForm.getDocumentId() + "]");
            }
        } else {
            document.setCustomText(documentFinancialGuarantorNaturalPersonForm.getCustomText());
        }
        documentRepository.save(document);
        documentService.initializeFieldsToProcessPdfGeneration(document);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.FINANCIAL);
        tenantService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenantRepository.save(tenant);
        return document;
    }

    private void deleteFilesIfExistedBefore(Document document) {
        if (document.getFiles() != null && !document.getFiles().isEmpty()) {
            document.setFiles(null);
            fileRepository.deleteAll(document.getFiles());
            ovhService.delete(document.getFiles().stream().map(File::getPath).collect(Collectors.toList()));
        }
    }
}

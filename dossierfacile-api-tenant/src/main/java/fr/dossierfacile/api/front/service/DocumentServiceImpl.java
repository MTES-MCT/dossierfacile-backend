package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.MinifyFileProducer;
import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.api.front.util.TransactionalUtil;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Person;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentAnalysisReportRepository documentAnalysisReportRepository;
    private final StorageFileRepository storageFileRepository;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;
    private final DocumentHelperService documentHelperService;
    private final MinifyFileProducer minifyFileProducer;
    private final Producer producer;

    @Override
    @Transactional
    public void changeDocumentStatus(Document document, DocumentStatus newStatus) {
        document.setDocumentStatus(newStatus);

        if (newStatus == DocumentStatus.TO_PROCESS) {
            document.setDocumentDeniedReasons(null);
            Tenant tenantOfDocument = Optional.ofNullable(document.getTenant()).orElseGet(() -> document.getGuarantor().getTenant());

            List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX);
            if (categoriesToChange.contains(document.getDocumentCategory())) {
                if (tenantOfDocument.getStatus() == TenantFileStatus.VALIDATED) {
                    List<Document> documentList = Optional.<Person>ofNullable(document.getTenant()).orElse(document.getGuarantor()).getDocuments();
                    resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(documentList, categoriesToChange);
                }
            }
            documentRepository.save(document);
            tenantStatusService.updateTenantStatus(tenantOfDocument);
            apartmentSharingService.resetDossierPdfGenerated(tenantOfDocument.getApartmentSharing());
        } else {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    @Override
    @Transactional
    public void delete(Document document) {
        Person ownerOfDocument = Optional.<Person>ofNullable(document.getTenant()).orElse(document.getGuarantor());
        Tenant tenantOfDocument = Optional.ofNullable(document.getTenant()).orElseGet(() -> document.getGuarantor().getTenant());

        List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX);
        if (categoriesToChange.contains(document.getDocumentCategory())) {
            if (tenantOfDocument.getStatus() == TenantFileStatus.VALIDATED) {
                List<Document> documentList = ownerOfDocument.getDocuments();
                resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(documentList, categoriesToChange);
            }
        }

        ownerOfDocument.getDocuments().removeIf(d -> Objects.equals(d.getId(), document.getId()));
        documentRepository.delete(document);
        tenantStatusService.updateTenantStatus(tenantOfDocument);
        apartmentSharingService.resetDossierPdfGenerated(tenantOfDocument.getApartmentSharing());
    }

    @Override
    @Transactional
    public void delete(Long documentId, Tenant referenceTenant) {
        Document document = documentRepository.findByIdForApartmentSharing(documentId, referenceTenant.getApartmentSharing().getId())
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        delete(document);
    }

    @Override
    @Transactional
    public void resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(List<Document> documentList, List<DocumentCategory> categoriesToChange) {
        Optional.ofNullable(documentList)
                .orElse(new ArrayList<>())
                .forEach(document -> {
                    if (document.getDocumentStatus().equals(DocumentStatus.VALIDATED)
                            && categoriesToChange.contains(document.getDocumentCategory())) {
                        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
                        document.setDocumentDeniedReasons(null);
                        documentRepository.save(document);
                    }
                });
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public void addFile(MultipartFile multipartFile, Document document) throws IOException {
        File file = documentHelperService.addFile(multipartFile, document);
        markDocumentAsEdited(document);

        TransactionalUtil.afterCommit(() -> {
            minifyFileProducer.minifyFile(file.getId());
            producer.analyzeFile(file);
        });
    }

    @Override
    public void markDocumentAsEdited(Document document) {
        document.setLastModifiedDate(LocalDateTime.now());
        if ( document.getDocumentAnalysisReport() != null) {
            documentAnalysisReportRepository.delete(document.getDocumentAnalysisReport());
            document.setDocumentAnalysisReport(null);
        }
        if ( document.getWatermarkFile() != null ){
            StorageFile watermarkFile = document.getWatermarkFile();
            storageFileRepository.delete(watermarkFile);
            document.setWatermarkFile(null);
        }
        documentRepository.save(document);
    }

}

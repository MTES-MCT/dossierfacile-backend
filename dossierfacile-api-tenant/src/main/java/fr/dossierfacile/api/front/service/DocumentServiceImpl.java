package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Person;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;

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
    public void delete(Long documentId, Tenant referenceTenant) {
        Document document = documentRepository.findByIdForApartmentSharing(documentId, referenceTenant.getApartmentSharing().getId())
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        Person ownerOfDocument = Optional.<Person>ofNullable(document.getTenant()).orElse(document.getGuarantor());
        Tenant tenantOfDocument = Optional.ofNullable(document.getTenant()).orElseGet(() -> document.getGuarantor().getTenant());

        List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX);
        if (categoriesToChange.contains(document.getDocumentCategory())) {
            if (tenantOfDocument.getStatus() == TenantFileStatus.VALIDATED) {
                List<Document> documentList = ownerOfDocument.getDocuments();
                resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(documentList, categoriesToChange);
            }
        }

        fileStorageService.delete(document.getFiles().stream().map(File::getPath).collect(Collectors.toList()));
        ownerOfDocument.getDocuments().removeIf(d -> d.getId() == document.getId());
        documentRepository.delete(document);
        tenantStatusService.updateTenantStatus(tenantOfDocument);
        apartmentSharingService.resetDossierPdfGenerated(tenantOfDocument.getApartmentSharing());

    }

    @Override
    public void initializeFieldsToProcessPdfGeneration(Document document) {
        document.setName(null);
        document.setProcessingStartTime(LocalDateTime.now());
        document.setProcessingEndTime(null);
        document.setLocked(false);
        document.setLockedBy(null);
        document.setRetries(0);
        documentRepository.save(document);
    }

    @Override
    public void initializeFieldsToProcessPdfGeneration(long documentId) {
        Document document = documentRepository.findById(documentId).orElseThrow(() -> new DocumentNotFoundException(documentId));
        initializeFieldsToProcessPdfGeneration(document);
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

    @Override
    public void deleteAllDocumentsAssociatedToTenant(Tenant tenant) {
        List<Document> documentList = documentRepository.findAllAssociatedToTenantId(tenant.getId());
        Optional.ofNullable(documentList)
                .orElse(new ArrayList<>())
                .forEach(this::deleteFilesFromStorage);

        documentRepository.deleteAll(Optional.ofNullable(documentList)
                .orElse(new ArrayList<>()));
    }

    private void deleteFilesFromStorage(Document document) {
        List<String> pathFiles = fileRepository.getFilePathsByDocumentId(document.getId());
        if (pathFiles != null && !pathFiles.isEmpty()) {
            log.info("Removing files from storage of document with id [" + document.getId() + "]");
            fileStorageService.delete(pathFiles);
        }
        if (document.getName() != null && !document.getName().isBlank()) {
            log.info("Removing document from storage with path [" + document.getName() + "]");
            fileStorageService.delete(document.getName());
        }
    }
}

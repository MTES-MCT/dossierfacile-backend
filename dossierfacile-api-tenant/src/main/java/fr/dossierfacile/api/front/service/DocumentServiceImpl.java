package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final TenantService tenantService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    public void delete(Long documentId, Tenant tenant) {
        Document document = documentRepository.findByIdAssociatedToTenantId(documentId, tenant.getId())
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        List<Document> documentList = new ArrayList<>();
        if (document.getTenant() != null) {
            documentList = document.getTenant().getDocuments();
        } else if (document.getGuarantor() != null) {
            documentList = document.getGuarantor().getDocuments();
        }

        if (document.getDocumentCategory() == DocumentCategory.PROFESSIONAL) {
            resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(documentList, List.of(DocumentCategory.FINANCIAL, DocumentCategory.TAX));
        } else if (document.getDocumentCategory() == DocumentCategory.FINANCIAL) {
            resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(documentList, List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.TAX));
        } else if (document.getDocumentCategory() == DocumentCategory.TAX) {
            resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(documentList, List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL));
        }

        fileStorageService.delete(document.getFiles().stream().map(File::getPath).collect(Collectors.toList()));
        documentRepository.delete(document);

        if (document.getTenant() != null) {
            tenant.getDocuments().remove(document);
        } else if (document.getGuarantor() != null) {
            tenant.getGuarantors().stream().filter(g -> Objects.equals(document.getGuarantor().getId(), g.getId())).findFirst().ifPresent(guarantor -> guarantor.getDocuments().remove(document));
        }

        tenantService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
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
    public void resetValidatedDocumentsStatusToToProcess(Tenant tenant) {
        if (tenant.getStatus().equals(TenantFileStatus.VALIDATED)) {
            Optional.ofNullable(tenant.getDocuments())
                    .orElse(new ArrayList<>())
                    .forEach(document -> {
                        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
                        document.setDocumentDeniedReasons(null);
                        documentRepository.save(document);
                    });
            Optional.ofNullable(tenant.getGuarantors())
                    .orElse(new ArrayList<>())
                    .forEach(guarantor -> Optional.ofNullable(guarantor.getDocuments())
                            .orElse(new ArrayList<>())
                            .forEach(document -> {
                                document.setDocumentStatus(DocumentStatus.TO_PROCESS);
                                document.setDocumentDeniedReasons(null);
                                documentRepository.save(document);
                            }));

        }
    }

    @Override
    public void resetValidatedAndDeniedDocumentsStatusToToProcess(List<Document> documentList) {
        Optional.ofNullable(documentList)
                .orElse(new ArrayList<>())
                .forEach(document -> {
                    if (!document.getDocumentStatus().equals(DocumentStatus.TO_PROCESS)) {
                        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
                        document.setDocumentDeniedReasons(null);
                        documentRepository.save(document);
                    }
                });
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

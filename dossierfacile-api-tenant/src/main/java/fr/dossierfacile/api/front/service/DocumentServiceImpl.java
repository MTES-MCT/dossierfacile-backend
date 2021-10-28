package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.service.interfaces.OvhService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final AuthenticationFacade authenticationFacade;
    private final OvhService ovhService;
    private final TenantService tenantService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    public void delete(Long documentId, Tenant tenant) {
        Document document = documentRepository.findByIdAssociatedToTenantId(documentId, tenant.getId())
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        resetValidatedDocumentsStatusToToProcess(tenant);
        ovhService.delete(document.getFiles().stream().map(File::getPath).collect(Collectors.toList()));
        documentRepository.delete(document);
        tenant.getDocuments().remove(document);
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
                        documentRepository.save(document);
                    });
            Optional.ofNullable(tenant.getGuarantors())
                    .orElse(new ArrayList<>())
                    .forEach(guarantor -> Optional.ofNullable(guarantor.getDocuments())
                            .orElse(new ArrayList<>())
                            .forEach(document -> {
                                document.setDocumentStatus(DocumentStatus.TO_PROCESS);
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
                        documentRepository.save(document);
                    }
                });
    }
}

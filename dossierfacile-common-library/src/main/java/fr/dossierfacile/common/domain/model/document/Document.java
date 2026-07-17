package fr.dossierfacile.common.domain.model.document;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;

import fr.dossierfacile.common.domain.model.DomainAggregate;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Aggregate Root pour le concept de Document.
 * Protège les invariants et expose la logique métier liée aux documents.
 */
@SuppressWarnings("ClassCanBeRecord")
public class Document implements Serializable, DomainAggregate<DocumentEntity> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final DocumentEntity entity;

    public Document(DocumentEntity entity) {
        this.entity = entity;
    }

    @Override
    public DocumentEntity getEntityOnlyForRepository() {
        return this.entity;
    }

    // --- ACCESSEURS (LECTURE SEULE POUR PROJECTIONS ET USE CASES) ---

    public Long getId() {
        return entity.getId();
    }

    public DocumentCategory getDocumentCategory() {
        return entity.getDocumentCategory();
    }

    public DocumentSubCategory getDocumentSubCategory() {
        return entity.getDocumentSubCategory();
    }

    public Long getTenantId() {
        return entity.getTenantId();
    }

    public Long getGuarantorId() {
        return entity.getGuarantorId();
    }

    public Boolean getNoDocument() {
        return entity.getNoDocument();
    }

    public DocumentStatus getDocumentStatus() {
        return entity.getDocumentStatus();
    }

    public List<FileEntity> getFiles() {
        return entity.getFiles();
    }

    public FileEntity getFileById(Long fileId) {
        return entity.getFiles().stream()
                .filter(file -> file.getId().equals(fileId))
                .findFirst()
                .orElseThrow(NullPointerException::new);
    }

    public void deleteFile(Long fileId) {
        // Nous trouvons le fichier à supprimer et marquons le fichier de stockage associé avec le statut "TO_DELETE"
        entity.getFiles().stream()
                .filter(file -> file.getId().equals(fileId))
                .findFirst()
                .ifPresent(file -> {
                    file.setDocument(null);
                    if (file.getStorageFile() != null) {
                        file.getStorageFile().setStatus(FileStorageStatus.TO_DELETE);
                    }
                    if (file.getPreview() != null) {
                        file.getPreview().setStatus(FileStorageStatus.TO_DELETE);
                    }
                });
        entity.getFiles().removeIf(file -> file.getId().equals(fileId));

        // Il faut ensuite supprimer le fichier watermark associé s'il existe
        if (entity.getWatermarkFile() != null) {
            entity.getWatermarkFile().setStatus(FileStorageStatus.TO_DELETE);
            entity.setWatermarkFile(null);
        }

        // Si on a des fichiers encore on met a jour la date de modification et le statut du document pour qu'il soit re-analysé
        if (hasFiles()) {
            entity.setLastModifiedDate(LocalDateTime.now(ZoneId.systemDefault()));
            entity.setDocumentStatus(DocumentStatus.TO_PROCESS);
        }
    }

    public void resetValidateOrInProgressDocumentAfterFileDeleted() {
        List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX);
        if (
                entity.getDocumentStatus() != null
                        && entity.getDocumentStatus() != DocumentStatus.DECLINED
                        && categoriesToChange.contains(entity.getDocumentCategory())
        ) {
            if (entity.getDocumentStatus() == DocumentStatus.VALIDATED) {
                entity.setDocumentStatus(DocumentStatus.TO_PROCESS);
                entity.setDocumentDeniedReasons(null);
            }
            // Ces deux dernières actions je suis pas sur qu'il y ai un intérêt de les faire
            if (Boolean.TRUE.equals(entity.getNoDocument()) && entity.getWatermarkFile() != null) {
                entity.getWatermarkFile().setStatus(FileStorageStatus.TO_DELETE);
                entity.setWatermarkFile(null);
            }
        }
    }

    public boolean hasFiles() {
        return !entity.getFiles().isEmpty();
    }
}

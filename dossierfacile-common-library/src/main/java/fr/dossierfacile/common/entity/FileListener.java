package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.utils.BeanUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PreRemove;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileListener {

    @PreRemove
    public void preRemove(File file) {
        try {
            EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
            detachAnalyses(file, entityManager);
        } catch (Exception e) {
            log.error("Unable to detach analyses from file {}", file.getId(), e);
        }
    }

    private void detachAnalyses(File file, EntityManager entityManager) {
        Long fileId = file.getId();
        Document document = file.getDocument();
        Long documentId = document != null ? document.getId() : null;

        // Blurry
        BlurryFileAnalysis blurry = file.getBlurryFileAnalysis();
        if (blurry != null && blurry.getFile() != null) {
            if (blurry.getDataFileId() == null) blurry.setDataFileId(fileId);
            if (blurry.getDataDocumentId() == null) blurry.setDataDocumentId(documentId);
            blurry.setFile(null);
            entityManager.merge(blurry);
        }

        // BarCode
        BarCodeFileAnalysis barCode = file.getFileAnalysis();
        if (barCode != null && barCode.getFile() != null) {
            barCode.setFile(null);
            entityManager.merge(barCode);
        }

        // ParsedFile
        ParsedFileAnalysis parsed = file.getParsedFileAnalysis();
        if (parsed != null && parsed.getFile() != null) {
            parsed.setFile(null);
            entityManager.merge(parsed);
        }

        // Metadata
        FileMetadata metadata = file.getFileMetadata();
        if (metadata != null && metadata.getFile() != null) {
            metadata.setFile(null);
            entityManager.remove(metadata);
        }
    }
}

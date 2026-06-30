package fr.dossierfacile.common.domain.model.document;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

    @Test
    void should_get_file_by_id() {
        FileEntity file1 = FileEntity.builder().id(100L).build();
        FileEntity file2 = FileEntity.builder().id(200L).build();
        DocumentEntity entity = DocumentEntity.builder()
                .files(List.of(file1, file2))
                .build();
        Document document = new Document(entity);

        assertThat(document.getFileById(200L)).isEqualTo(file2);
    }

    @Test
    void should_throw_exception_when_file_not_found_by_id() {
        FileEntity file1 = FileEntity.builder().id(100L).build();
        DocumentEntity entity = DocumentEntity.builder()
                .files(List.of(file1))
                .build();
        Document document = new Document(entity);

        assertThatThrownBy(() -> document.getFileById(999L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_delete_file_and_update_status_when_files_remain() {
        StorageFile storageFile = StorageFile.builder().status(FileStorageStatus.TEMPORARY).build();
        StorageFile previewFile = StorageFile.builder().status(FileStorageStatus.TEMPORARY).build();
        FileEntity fileToDelete = FileEntity.builder()
                .id(100L)
                .storageFile(storageFile)
                .preview(previewFile)
                .build();
        FileEntity otherFile = FileEntity.builder()
                .id(200L)
                .build();

        StorageFile watermark = StorageFile.builder().status(FileStorageStatus.TEMPORARY).build();

        DocumentEntity entity = DocumentEntity.builder()
                .files(new ArrayList<>(List.of(fileToDelete, otherFile)))
                .watermarkFile(watermark)
                .documentStatus(DocumentStatus.VALIDATED)
                .build();

        fileToDelete.setDocument(entity);
        otherFile.setDocument(entity);

        Document document = new Document(entity);

        document.deleteFile(100L);

        // Verify file is disassociated and removed from list
        assertThat(fileToDelete.getDocument()).isNull();
        assertThat(entity.getFiles()).containsExactly(otherFile);

        // Verify storage files are marked to delete
        assertThat(storageFile.getStatus()).isEqualTo(FileStorageStatus.TO_DELETE);
        assertThat(previewFile.getStatus()).isEqualTo(FileStorageStatus.TO_DELETE);
        assertThat(watermark.getStatus()).isEqualTo(FileStorageStatus.TO_DELETE);
        assertThat(entity.getWatermarkFile()).isNull();

        // Verify document is set to process
        assertThat(entity.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(entity.getLastModifiedDate()).isNotNull();
    }

    @Test
    void should_reset_validated_document_when_category_is_financial() {
        StorageFile watermark = StorageFile.builder().status(FileStorageStatus.TEMPORARY).build();
        DocumentEntity entity = DocumentEntity.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentStatus(DocumentStatus.VALIDATED)
                .documentDeniedReasons(fr.dossierfacile.common.entity.DocumentDeniedReasons.builder().build())
                .watermarkFile(watermark)
                .noDocument(true)
                .build();
        Document document = new Document(entity);

        document.resetValidateOrInProgressDocumentAfterFileDeleted();

        assertThat(entity.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(entity.getDocumentDeniedReasons()).isNull();
        assertThat(watermark.getStatus()).isEqualTo(FileStorageStatus.TO_DELETE);
        assertThat(entity.getWatermarkFile()).isNull();
    }

    @Test
    void should_not_reset_declined_document_on_resetValidateOrInProgressDocumentAfterFileDeleted() {
        DocumentEntity entity = DocumentEntity.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentStatus(DocumentStatus.DECLINED)
                .build();
        Document document = new Document(entity);

        document.resetValidateOrInProgressDocumentAfterFileDeleted();

        assertThat(entity.getDocumentStatus()).isEqualTo(DocumentStatus.DECLINED);
    }
}

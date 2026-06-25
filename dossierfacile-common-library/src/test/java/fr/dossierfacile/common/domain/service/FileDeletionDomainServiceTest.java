package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDeletionDomainServiceTest {

    private FileDeletionDomainService fileDeletionDomainService;

    @Mock
    private AddLogDomainService addLogDomainService;

    @Mock
    private JpaDocumentRepository jpaDocumentRepository;

    @Mock
    private MessagePublisher messagePublisher;

    @BeforeEach
    void setUp() {
        fileDeletionDomainService = new FileDeletionDomainService(
                addLogDomainService,
                jpaDocumentRepository,
                messagePublisher
        );
    }

    @Test
    void should_delete_file_and_keep_document_when_multiple_files_exist() {
        // Given
        TenantEntity tenantEntity = TenantEntity.builder().id(1L).build();
        Tenant targetTenant = new Tenant(tenantEntity);

        StorageFile storageFile = StorageFile.builder().status(FileStorageStatus.TEMPORARY).build();
        FileEntity fileToDelete = FileEntity.builder().id(100L).storageFile(storageFile).build();
        FileEntity otherFile = FileEntity.builder().id(200L).build();

        List<FileEntity> files = new ArrayList<>(List.of(fileToDelete, otherFile));
        DocumentEntity documentEntity = DocumentEntity.builder()
                .id(10L)
                .tenantId(1L)
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .files(files)
                .build();
        Document document = new Document(documentEntity);

        fileToDelete.setDocument(documentEntity);
        otherFile.setDocument(documentEntity);

        // When
        Optional<Document> result = fileDeletionDomainService.deleteFile(100L, document, targetTenant);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFiles()).hasSize(1);
        assertThat(result.get().getFiles().getFirst().getId()).isEqualTo(200L);
        assertThat(storageFile.getStatus()).isEqualTo(FileStorageStatus.TO_DELETE);

        verify(addLogDomainService).addFileDeletedLog(fileToDelete, targetTenant);
        verify(jpaDocumentRepository).save(document);
        verify(jpaDocumentRepository, never()).delete(any());
        verify(messagePublisher).sendDocumentForPdfGeneration(10L);
    }

    @Test
    void should_delete_file_and_delete_document_when_it_was_the_last_file() {
        // Given
        TenantEntity tenantEntity = TenantEntity.builder().id(1L).build();
        Tenant targetTenant = new Tenant(tenantEntity);

        StorageFile storageFile = StorageFile.builder().status(FileStorageStatus.TEMPORARY).build();
        FileEntity fileToDelete = FileEntity.builder().id(100L).storageFile(storageFile).build();

        List<FileEntity> files = new ArrayList<>(List.of(fileToDelete));
        DocumentEntity documentEntity = DocumentEntity.builder()
                .id(10L)
                .tenantId(1L)
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .files(files)
                .build();
        Document document = new Document(documentEntity);
        fileToDelete.setDocument(documentEntity);

        DocumentEntity otherDocumentEntity = DocumentEntity.builder()
                .id(20L)
                .tenantId(1L)
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .noDocument(true)
                .build();
        Document otherDocument = new Document(otherDocumentEntity);

        when(jpaDocumentRepository.getDocumentsByTenantId(1L)).thenReturn(List.of(otherDocument));

        // When
        Optional<Document> result = fileDeletionDomainService.deleteFile(100L, document, targetTenant);

        // Then
        assertThat(result).isEmpty();
        assertThat(storageFile.getStatus()).isEqualTo(FileStorageStatus.TO_DELETE);

        verify(addLogDomainService).addFileDeletedLog(fileToDelete, targetTenant);
        verify(addLogDomainService).addDocumentDeletedLog(document, targetTenant);
        verify(jpaDocumentRepository).getDocumentsByTenantId(1L);
        verify(messagePublisher).sendDocumentForPdfGeneration(20L);
        verify(jpaDocumentRepository).delete(document);
    }
}

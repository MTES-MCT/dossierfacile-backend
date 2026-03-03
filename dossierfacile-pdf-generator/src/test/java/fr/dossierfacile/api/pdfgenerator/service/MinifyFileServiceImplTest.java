package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.repository.FileRepository;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinifyFileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private StorageFileRepository storageFileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentHelperService documentHelperService;

    private MinifyFileServiceImpl minifyFileService;

    @BeforeEach
    void setUp() {
        minifyFileService = new MinifyFileServiceImpl(fileRepository, storageFileRepository, fileStorageService, documentHelperService);
    }

    @Test
    void process_shouldHardDeletePreview_whenOptimisticLockOccurs() {
        StorageFile sourceFile = storageFileWithIdAndName(10L, "input.pdf");
        StorageFile newPreview = storageFileWithIdAndName(20L, "preview.jpg");
        File file = buildFile(1L, sourceFile, null);
        InputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});

        when(documentHelperService.generatePreview(any(Document.class), any(InputStream.class), eq("input.pdf")))
                .thenReturn(newPreview);
        when(fileRepository.saveAndFlush(file))
                .thenThrow(new ObjectOptimisticLockingFailureException(File.class, 1L));

        minifyFileService.process(inputStream, file);

        verify(fileRepository).saveAndFlush(file);
        verify(fileStorageService).hardDelete(newPreview);
        verify(storageFileRepository).save(newPreview);
        verify(fileStorageService, never()).delete(any(StorageFile.class));
    }

    @Test
    void process_shouldSavePreview_andReplacePreviousPreview_onHappyPath() {
        StorageFile sourceFile = storageFileWithIdAndName(11L, "input.pdf");
        StorageFile previousPreview = storageFileWithIdAndName(30L, "old-preview.jpg");
        StorageFile newPreview = storageFileWithIdAndName(31L, "new-preview.jpg");
        File file = buildFile(2L, sourceFile, previousPreview);
        InputStream inputStream = new ByteArrayInputStream(new byte[]{4, 5, 6});

        when(documentHelperService.generatePreview(any(Document.class), any(InputStream.class), eq("input.pdf")))
                .thenReturn(newPreview);
        when(fileRepository.saveAndFlush(file)).thenReturn(file);

        minifyFileService.process(inputStream, file);

        verify(fileStorageService).delete(previousPreview);
        verify(fileRepository).saveAndFlush(file);
        verify(fileStorageService, never()).hardDelete(any(StorageFile.class));
        verify(storageFileRepository, never()).save(any(StorageFile.class));
    }

    private static StorageFile storageFileWithIdAndName(Long id, String name) {
        StorageFile storageFile = new StorageFile();
        storageFile.setId(id);
        storageFile.setName(name);
        return storageFile;
    }

    private static File buildFile(Long id, StorageFile storageFile, StorageFile preview) {
        File file = new File();
        file.setId(id);
        file.setStorageFile(storageFile);
        file.setPreview(preview);
        file.setDocument(new Document());
        return file;
    }
}

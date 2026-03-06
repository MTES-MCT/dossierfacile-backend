package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.repository.FileRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.MinifyFileService;
import fr.dossierfacile.api.pdfgenerator.service.processor.MetadataFileProcessor;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.service.FileStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessFileServiceImplTest {

    @Mock
    private MinifyFileService minifyFileService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileStorageServiceImpl fileStorageService;

    @Mock
    private MetadataFileProcessor metadataFileProcessor;

    @InjectMocks
    private ProcessFileServiceImpl processFileService;

    @Test
    void process_shouldCallBothProcessors_onHappyPath() throws Exception {
        File file = buildFile(42L);
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));
        when(fileStorageService.download(file.getStorageFile())).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        processFileService.process(42L);

        verify(minifyFileService).process(any(InputStream.class), eq(file));
        verify(metadataFileProcessor).process(any(InputStream.class), eq(file));
    }

    @Test
    void process_shouldContinueWithMetadata_whenMinifyThrows() throws Exception {
        File file = buildFile(43L);
        when(fileRepository.findById(43L)).thenReturn(Optional.of(file));
        when(fileStorageService.download(file.getStorageFile())).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        doThrow(new RuntimeException("minify failed"))
                .when(minifyFileService).process(any(InputStream.class), eq(file));

        assertDoesNotThrow(() -> processFileService.process(43L));

        verify(minifyFileService).process(any(InputStream.class), eq(file));
        verify(metadataFileProcessor).process(any(InputStream.class), eq(file));
    }

    @Test
    void process_shouldContinueWithMinify_whenMetadataThrows() throws Exception {
        File file = buildFile(44L);
        when(fileRepository.findById(44L)).thenReturn(Optional.of(file));
        when(fileStorageService.download(file.getStorageFile())).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        doThrow(new RuntimeException("metadata failed"))
                .when(metadataFileProcessor).process(any(InputStream.class), eq(file));

        assertDoesNotThrow(() -> processFileService.process(44L));

        verify(minifyFileService).process(any(InputStream.class), eq(file));
        verify(metadataFileProcessor).process(any(InputStream.class), eq(file));
    }

    @Test
    void process_shouldThrowWhenFileDoesNotExist() {
        when(fileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> processFileService.process(99L));
    }

    private static File buildFile(Long id) {
        StorageFile storageFile = new StorageFile();
        storageFile.setId(id + 100);
        storageFile.setName("source.pdf");

        File file = new File();
        file.setId(id);
        file.setStorageFile(storageFile);
        file.setDocument(new Document());
        return file;
    }
}

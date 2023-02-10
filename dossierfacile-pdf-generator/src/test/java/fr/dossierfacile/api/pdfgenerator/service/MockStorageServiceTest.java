package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.repository.EncryptionKeyRepository;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.EncryptionKeyServiceImpl;
import fr.dossierfacile.common.service.MockStorage;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@ExtendWith(MockitoExtension.class)
class MockStorageServiceTest {

    @Mock
    EncryptionKeyRepository repository;
    @Mock
    StorageFileRepository storageFileRepository;
    @InjectMocks
    EncryptionKeyServiceImpl encryptionKeyService;

    MockStorage fileStorageService;

    @BeforeEach
    void init() {
        fileStorageService = new MockStorage("./mockstorage/", storageFileRepository);
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArguments()[0]);
        Mockito.when(storageFileRepository.save(Mockito.any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void check_upload_download_with_mockstorage() throws IOException {
        EncryptionKey key = encryptionKeyService.getCurrentKey();

        StorageFile file = fileStorageService.upload(new ByteArrayInputStream(new byte[]{1, 2, 3}), StorageFile.builder().name("test").encryptionKey(key).build());
        InputStream result = fileStorageService.download(file.getPath(), key);
        Assertions.assertArrayEquals(new byte[]{1, 2, 3}, IOUtils.toByteArray(result));
    }
}
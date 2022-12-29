package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.repository.EncryptionKeyRepository;
import fr.dossierfacile.common.service.EncryptionKeyServiceImpl;
import fr.dossierfacile.common.service.MockStorage;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;

@ExtendWith(MockitoExtension.class)
class MockStorageServiceTest {

    @Mock
    EncryptionKeyRepository repository;

    @InjectMocks
    EncryptionKeyServiceImpl encryptionKeyService;

    MockStorage fileStorageService = new MockStorage("./mockstorage/");

    @Test
    void check_upload_download_with_mockstorage() throws IOException {
        Mockito.when(repository.save(Mockito.any())).thenAnswer(i -> i.getArguments()[0]);
        EncryptionKey key = encryptionKeyService.getCurrentKey();

        String filename = fileStorageService.uploadByteArray(new byte[]{1, 2, 3}, "test", key);
        InputStream result = fileStorageService.download(filename, key);
        Assertions.assertArrayEquals(new byte[]{1, 2, 3}, IOUtils.toByteArray(result));
    }
}
package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.EncryptionKeyStatus;
import fr.dossierfacile.common.service.interfaces.ThreeDSOutscaleFileStorageService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

@ExtendWith(MockitoExtension.class)
class ThreeDSOutscaleFileStorageServiceImplTest {

    @Mock
    ThreeDSOutscaleFileStorageService threeDSOutscaleFileStorageService;

    @Test
    @Disabled
    void connect3DS() throws IOException, NoSuchAlgorithmException {
        InputStream fileInputStream = ThreeDSOutscaleFileStorageServiceImplTest.class.getClassLoader().getResourceAsStream("hello.txt");
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        SecretKey key = keygen.generateKey();
        Key encryptionKey = EncryptionKey.builder()
                .algorithm(key.getAlgorithm())
                .format(key.getFormat())
                .encodedSecret(key.getEncoded())
                .status(EncryptionKeyStatus.CURRENT)
                .version(1)
                .build();
        long start = System.currentTimeMillis();
        threeDSOutscaleFileStorageService.upload("hello.txt", fileInputStream, encryptionKey, "application/txt");
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.println("time 1 " + timeElapsed);
    }

    @Test
    @Disabled
    void deleteFile() {
        long start = System.currentTimeMillis();
        threeDSOutscaleFileStorageService.delete("hello.txt");
        long finish2 = System.currentTimeMillis();
        long timeElapsed2 = finish2 - start;
        System.out.println("time 2 " + timeElapsed2);
    }
}

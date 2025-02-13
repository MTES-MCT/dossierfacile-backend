package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.exceptions.UnsupportedKeyException;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.file.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static java.lang.String.format;

@Slf4j
@Service
@Profile("mockOvh")
public class LocalMockStorage implements FileStorageProviderService {
    private final String filePath;

    public LocalMockStorage(@Value("${mock.storage.path:./mockstorage/}") String filePath) {
        this.filePath = filePath;
        new File(filePath).mkdirs();
    }

    @Override
    public ObjectStorageProvider getProvider() {
        return ObjectStorageProvider.LOCAL;
    }

    @Override
    public void delete(String path) {
        try {
            Files.delete(Path.of(filePath, path));
        } catch (NoSuchFileException e) {
            log.error(format("File %s does not exist", filePath + path), e);
        } catch (DirectoryNotEmptyException e) {
            log.error(format("File %s is a non empty directory", filePath + path), e);
        } catch (IOException e) {
            log.error(format("File %s cannot be deleted", filePath + path), e);
        }
    }

    @Override
    public InputStream download(String path, EncryptionKey key) throws IOException {
        InputStream in = Files.newInputStream(Path.of(filePath, path));
        if (key != null) {
            if (key.getVersion() != 2) {
                throw new UnsupportedKeyException("Unsupported key version " + key.getVersion());
            }
            try {
                byte[] iv = DigestUtils.sha256(path);
                GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.DECRYPT_MODE, key, gcmParamSpec);

                in = new CipherInputStream(in, aes);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                     InvalidAlgorithmParameterException e) {
                throw new IOException(e);
            }
        }
        return in;
    }

    @Override
    public void upload(String path, InputStream inputStream, EncryptionKey key, String contentType) throws RetryableOperationException, IOException {
        if (key != null) {
            try {
                byte[] iv = DigestUtils.sha256(path);
                GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.ENCRYPT_MODE, key, gcmParamSpec);

                inputStream = new CipherInputStream(inputStream, aes);
            } catch (Exception e) {
                log.error("Unable to encrypt file", e);
                throw new IOException(e);
            }
        }
        try {
            File file = Path.of(filePath, path).toFile();

            try (OutputStream outputStream = new FileOutputStream(file)) {
                IOUtils.copy(inputStream, outputStream);
            }
        } catch (Exception e) {
            log.error("Mock - Unable to uploadFile", e);
            throw new IOException(e);
        }
    }

    @Override
    public List<String> listObjectNames(@Nullable String marker, int maxObjects) {

        Path directory = Path.of(filePath);
        if (Files.isDirectory(directory)) {
            try {
                return Files.list(directory).map(Path::toString).toList();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("File path" + filePath + " is not a directory");
    }

}
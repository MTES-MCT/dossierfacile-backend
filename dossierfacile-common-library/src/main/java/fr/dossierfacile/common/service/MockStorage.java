package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.exceptions.UnsupportedKeyException;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.UUID;

import static java.lang.String.format;

@Slf4j
@Service
@Profile("mockOvh")
public class MockStorage implements FileStorageService {
    private final String filePath;
    private final StorageFileRepository storageFileRepository;

    public MockStorage(@Value("${mock.storage.path:./mockstorage/}") String filePath, StorageFileRepository storageFileRepository) {
        this.filePath = filePath;
        this.storageFileRepository = storageFileRepository;
        new File(filePath).mkdirs();
    }

    @Override
    public void delete(StorageFile storageFile) {
        try {
            Files.delete(Path.of(filePath + storageFile.getPath()));
        } catch (NoSuchFileException e) {
            log.error(format("File %s does not exist", filePath + storageFile.getPath()), e);
        } catch (DirectoryNotEmptyException e) {
            log.error(format("File %s is a non empty directory", filePath + storageFile.getPath()), e);
        } catch (IOException e) {
            log.error(format("File %s cannot be deleted", filePath + storageFile.getPath()), e);
        }
    }

    private InputStream download(String filename, EncryptionKey key) throws IOException {
        InputStream in = Files.newInputStream(Path.of(filePath + filename));
        if (key != null) {
            if (key.getVersion() != 1){
                throw new UnsupportedKeyException("Unsupported key version " + key.getVersion());
            }
            try {
                byte[] iv = DigestUtils.md5(filename);
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
    public InputStream download(StorageFile storageFile) throws IOException {
        return download(storageFile.getPath(), storageFile.getEncryptionKey());
    }

    public void upload(String ovhPath, InputStream inputStream, Key key, String contentType) throws IOException {
        if (key != null) {
            try {
                byte[] iv = DigestUtils.md5(ovhPath);
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
            File file = new File(filePath + ovhPath);

            try (OutputStream outputStream = new FileOutputStream(file)) {
                IOUtils.copy(inputStream, outputStream);
            }
        } catch (Exception e) {
            log.error("Mock - Unable to uploadFile", e);
            throw new IOException(e);
        }
    }

    @Override
    public StorageFile upload(InputStream inputStream, StorageFile storageFile) throws IOException {
        try {
            return uploadToProvider(inputStream, storageFile, ObjectStorageProvider.OVH);
        } catch (RetryableOperationException e) {
            return null;
        }
    }

    @Override
    public StorageFile uploadToProvider(InputStream inputStream, StorageFile storageFile, ObjectStorageProvider provider) throws RetryableOperationException, IOException {
        if (inputStream == null)
            return null;
        if (storageFile == null) {
            log.warn("fallback on uploadfile");
            storageFile = StorageFile.builder()
                    .name("undefined")
                    .provider(provider)
                    .providers(Collections.singletonList(provider.toString()))
                    .build();
        }

        if (StringUtils.isBlank(storageFile.getPath())) {
            storageFile.setPath(UUID.randomUUID().toString());
        }
        upload(storageFile.getPath(), inputStream, storageFile.getEncryptionKey(), storageFile.getContentType());

        return storageFileRepository.save(storageFile);

    }

}
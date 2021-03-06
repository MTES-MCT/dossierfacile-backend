package fr.dossierfacile.common.service;

import fr.dossierfacile.common.exceptions.FileCannotUploadedException;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;

@Slf4j
@Service
@Profile("mockOvh")
public class MockStorage implements FileStorageService {
    static String filePath = "./mockstorage/";


    public MockStorage() {
        new File(filePath).mkdirs();
    }

    @Override
    public void delete(String name) {
        try {
            Files.delete(Path.of(filePath + name));
        } catch (NoSuchFileException e) {
            log.error(format("File %s does not exist", filePath + name), e);
        } catch (DirectoryNotEmptyException e) {
            log.error(format("File %s is a non empty directory", filePath + name), e);
        } catch (IOException e) {
            log.error(format("File %s cannot be deleted", filePath + name), e);
        }
    }

    @Override
    public void delete(List<String> name) {
        name.forEach(this::delete);
    }

    @Override
    public void deleteAllFiles(String path) {
        throw new UnsupportedOperationException("deleteAllFiles");
    }

    @Override
    public InputStream download(String filename, Key key) throws IOException {
        InputStream in = Files.newInputStream(Path.of(filePath + filename));
        if (key != null) {
            try {
                Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
                aes.init(Cipher.DECRYPT_MODE, key);

                in = new CipherInputStream(in, aes);
            } catch (NoSuchPaddingException e) {
                throw new IOException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException(e);
            } catch (InvalidKeyException e) {
                throw new IOException(e);
            }
        }
        return in;
    }

    @Override
    public InputStream download(fr.dossierfacile.common.entity.File file) throws IOException {
        return download(file.getPath(), file.getKey());
    }

    @Override
    public void upload(String ovhPath, InputStream inputStream, Key key) throws IOException {
        if (key != null) {
            try {
                Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
                aes.init(Cipher.ENCRYPT_MODE, key);

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
    public String uploadFile(MultipartFile file, Key key) {
        String name = file.getOriginalFilename() + UUID.randomUUID() + "." + Objects.requireNonNull(FilenameUtils.getExtension(file.getOriginalFilename())).toLowerCase(Locale.ROOT);
        try {
            upload(name, file.getInputStream(), key);
        } catch (IOException e) {
            throw new FileCannotUploadedException();
        }
        return name;
    }
}
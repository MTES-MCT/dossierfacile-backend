package fr.dossierfacile.common.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import fr.dossierfacile.common.config.ThreeDSOutscaleConfig;
import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.shared.StoredFile;
import fr.dossierfacile.common.exceptions.FileCannotUploadedException;
import fr.dossierfacile.common.exceptions.OldKeyException;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.ThreeDSOutscaleFileStorageService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!mockOvh")
public class ThreeDSOutscaleFileStorageServiceImpl implements ThreeDSOutscaleFileStorageService {

    @Autowired
    private ThreeDSOutscaleConfig threeDSOutscaleConfig;
    @Autowired
    private StorageFileRepository storageFileRepository;

    @Value("${threeds.s3.bucket:dossierfacile-preprod}")
    private String bucket;

    @Override
    @Async
    public void delete(String name) {
        AmazonS3 s3client = threeDSOutscaleConfig.getAmazonS3Client();
        s3client.deleteObject(bucket, name);
    }

    @Override
    @Async
    public void delete(List<String> name) {
        name.forEach(this::delete);
    }

    @Override
    public InputStream download(String path, Key key) throws IOException {
        AmazonS3 s3client = threeDSOutscaleConfig.getAmazonS3Client();
        S3Object fullObject;
        try {
            fullObject = s3client.getObject(new GetObjectRequest(bucket, path));
            InputStream in = fullObject.getObjectContent();
            if (key != null) {
                in = cipherInputStream(path, key, in);
            }
            return in;
        } catch (AmazonServiceException e) {
            throw new FileNotFoundException("File " + path + " not found");
        }
    }

    private static InputStream cipherInputStream(String path, Key key, InputStream in) throws IOException {
        try {
            Cipher aes;
            if (key instanceof EncryptionKey && ((EncryptionKey) key).getVersion() == 0) {
                Sentry.captureMessage("Wrong key version used");
                throw new OldKeyException("Wrong key version used");
            } else {
                byte[] iv = DigestUtils.sha256(path); // arbitrary set the filename to build IV
                GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.DECRYPT_MODE, key, gcmParamSpec);
            }
            in = new CipherInputStream(in, aes);
        } catch (Exception e) {
            throw new IOException(e);
        }
        return in;
    }

    @Override
    public InputStream download(StoredFile file) throws IOException {
        return download(file.getPath(), file.getEncryptionKey());
    }

    public void upload(String name, InputStream inputStream, Key key, String contentType) throws IOException {
        if (key != null) {
            try {
                byte[] iv = DigestUtils.sha256(name);
                GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.ENCRYPT_MODE, key, gcmParamSpec);

                inputStream = new CipherInputStream(inputStream, aes);

            } catch (Exception e) {
                log.error("Unable to encrypt file", e);
                throw new IOException(e);
            }
        }

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(contentType);

        AmazonS3 s3client = threeDSOutscaleConfig.getAmazonS3Client();
        PutObjectResult putObjectResult = s3client.putObject(
                bucket,
                name,
                inputStream,
                objectMetadata);
        if (StringUtils.isEmpty(putObjectResult.getETag())) {
            throw new IOException("ETag is empty - download failed!" + name);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, Key key) {
        String name = UUID.randomUUID() + "." + Objects.requireNonNull(FilenameUtils.getExtension(file.getOriginalFilename())).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        try (InputStream is = file.getInputStream()) {
            upload(name, is, key, contentType);
        } catch (IOException e) {
            throw new FileCannotUploadedException();
        }
        return name;
    }

    @Override
    public StorageFile upload(InputStream inputStream, StorageFile storageFile) throws IOException {
        if (inputStream == null)
            return null;
        if (storageFile == null) {
            log.warn("fallback on uploadfile");
            storageFile = StorageFile.builder()
                    .name("undefined")
                    .provider(ObjectStorageProvider.THREEDS_OUTSCALE)
                    .build();
        }

        if (StringUtils.isBlank(storageFile.getPath())) {
            storageFile.setPath(UUID.randomUUID().toString());
        }
        upload(storageFile.getPath(), inputStream, storageFile.getEncryptionKey(), storageFile.getContentType());

        return storageFileRepository.save(storageFile);

    }
}

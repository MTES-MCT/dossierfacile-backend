package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.exceptions.FileCannotUploadedException;
import fr.dossierfacile.common.exceptions.OvhConnectionFailedException;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@Profile("!mockOvh")
public class OvhFileStorageServiceImpl implements FileStorageService {
    private static final String EXCEPTION = "Sentry ID Exception: ";

    @Value("${ovh.project.domain:default}")
    private String ovhProjectDomain;
    @Value("${ovh.auth.url:default}")
    private String ovhAuthUrl;
    @Value("${ovh.username:default}")
    private String ovhUsername;
    @Value("${ovh.password:default}")
    private String ovhPassword;
    @Value("${ovh.project.name:default}")
    private String ovhProjectName;
    @Value("${ovh.region:default}")
    private String ovhRegion;
    @Value("${ovh.container:default}")
    private String ovhContainerName;
    @Value("${ovh.connection.reattempts:3}")
    private Integer ovhConnectionReattempts;
    private String tokenId;

    private OSClient.OSClientV3 connect() {
        Identifier domainIdentifier = Identifier.byId(ovhProjectDomain);

        for (int i = 0; i <= ovhConnectionReattempts; i++) {
            try {
                OSClient.OSClientV3 os;
                if (tokenId == null) {
                    os = OSFactory.builderV3()
                            .endpoint(ovhAuthUrl)
                            .credentials(ovhUsername, ovhPassword, domainIdentifier)
                            .scopeToProject(Identifier.byName(ovhProjectName), Identifier.byName(ovhProjectDomain))
                            .authenticate();
                    os.useRegion(ovhRegion);
                    tokenId = os.getToken().getId();
                } else {
                    os = OSFactory.builderV3()
                            .endpoint(ovhAuthUrl)
                            .token(tokenId)
                            .scopeToProject(Identifier.byName(ovhProjectName), Identifier.byName(ovhProjectDomain))
                            .authenticate();
                    os.useRegion(ovhRegion);
                }
                return os;
            } catch (AuthenticationException | ClientResponseException e) {
                log.error("ObjectStorage authentication failed - reset tokenId. (" + i + "/" + ovhConnectionReattempts + ")" + EXCEPTION + Sentry.captureException(e), e);
                tokenId = null;
            } catch (Exception e) {
                log.error("ObjectStorage failed. (" + i + "/" + ovhConnectionReattempts + ")" + EXCEPTION + Sentry.captureException(e), e);
            }
        }
        throw new OvhConnectionFailedException("ObjectStorage Max attempts reached ");
    }

    @Override
    @Async
    public void delete(String name) {
        connect().objectStorage().objects().delete(ovhContainerName, name);
    }

    @Override
    @Async
    public void delete(List<String> name) {
        name.forEach(this::delete);
    }

    @Override
    public void deleteAllFiles(String path) {
        if (!path.contains("/")) {
            List<? extends SwiftObject> swiftObjects = getListObject(path);
            for (SwiftObject swiftObject : swiftObjects) {
                delete(swiftObject.getName());
            }
        } else {
            delete(path);
        }
    }

    @Override
    public InputStream download(String path, Key key) throws IOException {
        SwiftObject object = connect().objectStorage().objects().get(ovhContainerName, path);
        if (object == null)
            throw new FileNotFoundException("File " + path + " not found");

        InputStream in = object.download().getInputStream();
        if (key != null) {
            try {
                Cipher aes;
                if (key instanceof EncryptionKey && ((EncryptionKey) key).getVersion() == 0) {
                    aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    aes.init(Cipher.DECRYPT_MODE, key);
                } else {
                    byte[] iv = DigestUtils.md5(path); // arbitrary set the filename to build IV
                    GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                    aes = Cipher.getInstance("AES/GCM/NoPadding");
                    aes.init(Cipher.DECRYPT_MODE, key, gcmParamSpec);
                }
                in = new CipherInputStream(in, aes);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return in;
    }

    @Override
    public InputStream download(File file) throws IOException {
        return download(file.getPath(), file.getKey());
    }

    private List<? extends SwiftObject> getListObject(String folderName) {
        OSClient.OSClientV3 os = connect();
        return os.objectStorage().objects().list(ovhContainerName, ObjectListOptions.create()
                .path(folderName)
        );
    }

    @Override
    public void upload(String ovhPath, InputStream inputStream, Key key) throws IOException {
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
        String eTag = connect().objectStorage().objects().put(ovhContainerName, ovhPath, Payloads.create(inputStream));
        if (StringUtils.isEmpty(eTag)) {
            throw new IOException("ETag is empty - download failed!" + ovhPath);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, Key key) {
        String name = UUID.randomUUID() + "." + Objects.requireNonNull(FilenameUtils.getExtension(file.getOriginalFilename())).toLowerCase(Locale.ROOT);
        try (InputStream is = file.getInputStream()) {
            upload(name, is, key);
        } catch (IOException e) {
            throw new FileCannotUploadedException();
        }
        return name;
    }

    @Override
    public String uploadByteArray(byte[] file, String extension, Key key) {
        String name = UUID.randomUUID() + "." + Objects.requireNonNull(extension).toLowerCase(Locale.ROOT);
        try (InputStream targetStream = new ByteArrayInputStream(file)) {
            upload(name, targetStream, key);
        } catch (IOException e) {
            throw new FileCannotUploadedException();
        }
        return name;
    }
}
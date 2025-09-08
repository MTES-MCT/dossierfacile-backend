package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.exceptions.OvhConnectionFailedException;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.exceptions.UnsupportedKeyException;
import fr.dossierfacile.common.model.S3Bucket;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.api.exceptions.ConnectionException;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service("ovhFileStorageProvider")
@Slf4j
@Profile("!mockOvh")
@Deprecated
public class OvhFileStorageServiceImpl implements FileStorageProviderService {
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
    private final ThreadLocal<OSClient.OSClientV3> osClientThreadLocal = new ThreadLocal<>();

    private synchronized OSClient.OSClientV3 authenticate() {
        osClientThreadLocal.set(null);
        for (int i = 0; i <= ovhConnectionReattempts; i++) {
            try {
                osClientThreadLocal.set(OSFactory.builderV3()
                        .endpoint(ovhAuthUrl)
                        .credentials(ovhUsername, ovhPassword, Identifier.byId(ovhProjectDomain))
                        .scopeToProject(Identifier.byName(ovhProjectName), Identifier.byName(ovhProjectDomain))
                        .authenticate());
                osClientThreadLocal.get().useRegion(ovhRegion);

            } catch (AuthenticationException | ClientResponseException e) {
                log.error("ObjectStorage authentication failed.", e);
                break;
            } catch (ConnectionException e) {
                log.error("ObjectStorage failed. (" + i + "/" + ovhConnectionReattempts + ")", e);
            }
        }
        if (osClientThreadLocal.get() == null) {
            throw new OvhConnectionFailedException("ObjectStorage Max attempts reached ");
        }
        return osClientThreadLocal.get();
    }

    private OSClient.OSClientV3 getClient() {
        if (osClientThreadLocal.get() != null) return osClientThreadLocal.get();
        return authenticate();
    }

    // TODO will be put in common abstract class when key version 1 will be deprecated
    private static InputStream cipherInputStream(String path, EncryptionKey key, InputStream in) throws IOException {
        try {
            Cipher aes;
            byte[] iv = (key.getVersion() == 1) ? DigestUtils.md5(path) : DigestUtils.sha256(path);
            if (key.getVersion() == 1 || key.getVersion() == 2) {
                GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.DECRYPT_MODE, key, gcmParamSpec);
            } else {
                throw new UnsupportedKeyException("Unsupported Key version " + key.getVersion());
            }
            in = new CipherInputStream(in, aes);
        } catch (Exception e) {
            throw new IOException(e);
        }
        return in;
    }

    @Override
    public ObjectStorageProvider getProvider() {
        return ObjectStorageProvider.OVH;
    }

    @Override
    public void delete(String path) {
        try {
            getClient().objectStorage().objects().delete(ovhContainerName, path);
        } catch (AuthenticationException e) {
            log.error("ObjectStorage authentication failed.", e);
            authenticate().objectStorage().objects().delete(ovhContainerName, path);
        }
    }

    @Override
    public InputStream download(String path, EncryptionKey key) throws IOException {
        SwiftObject object;
        try {
            object = getClient().objectStorage().objects().get(ovhContainerName, path);
        } catch (AuthenticationException e) {
            log.error("ObjectStorage authentication failed.", e);
            object = authenticate().objectStorage().objects().get(ovhContainerName, path);
        }

        if (object == null) throw new FileNotFoundException("File " + path + " not found");

        InputStream in = object.download().getInputStream();
        if (key != null) {
            in = cipherInputStream(path, key, in);
        }
        return in;
    }

    @Override
    public void upload(String path, InputStream inputStream, EncryptionKey key, String contentType) throws RetryableOperationException, IOException {
        if (key != null) {
            if (key.getVersion() != 1 && key.getVersion() != 2){
                throw new UnsupportedKeyException("Unsupported key version " + key.getVersion());
            }
            try {
                byte[] iv = (key.getVersion() == 1) ? DigestUtils.md5(path) : DigestUtils.sha256(path);
                GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.ENCRYPT_MODE, key, gcmParamSpec);

                inputStream = new CipherInputStream(inputStream, aes);

            } catch (Exception e) {
                log.error("Unable to encrypt file", e);
                throw new RetryableOperationException("Unable to encrypt file", e);
            }
        }

        String eTag;
        Payload<InputStream> payload = Payloads.create(inputStream);
        try {
            eTag = getClient().objectStorage().objects().put(ovhContainerName, path, payload);
            SwiftObject metaData = getClient().objectStorage().objects().get(ovhContainerName, path);
            if (metaData.getSizeInBytes() <= 0) {
                throw new IOException("File size is null - upload failed for: " + path);
            }

        } catch (AuthenticationException e) {
            log.error("ObjectStorage authentication failed.", e);
            eTag = authenticate().objectStorage().objects().put(ovhContainerName, path, payload);
        } catch (OvhConnectionFailedException e) {
            throw new RetryableOperationException("Ovh Connection Failed", e);
        }
        if (StringUtils.isEmpty(eTag)) {
            throw new IOException("ETag is empty - upload failed!" + path);
        }
    }

    @Override
    public List<String> listObjectNames(@Nullable String marker, int maxObjects) {
        ObjectListOptions options = ObjectListOptions.create().limit(maxObjects);
        if (marker != null) {
            options.marker(marker);
        }
        return getClient().objectStorage().objects()
                .list(ovhContainerName, options)
                .stream()
                .map(SwiftObject::getName)
                .toList();
    }

    @Override
    public void uploadV2(S3Bucket s3Bucket, String fileKey, InputStream inputStream, String contentType, EncryptionKey key) throws RetryableOperationException, IOException {
        throw new NotImplementedException();
    }

    @Override
    public InputStream downloadV2(S3Bucket bucket, String path, EncryptionKey key) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void deleteV2(S3Bucket bucket, String path) {
        throw new NotImplementedException("OVH does not support deleteV2 operation");
    }

}
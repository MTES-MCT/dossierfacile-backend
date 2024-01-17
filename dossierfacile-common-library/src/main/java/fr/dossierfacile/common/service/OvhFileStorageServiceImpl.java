package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.exceptions.OvhConnectionFailedException;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.exceptions.UnsupportedKeyException;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.List;

@Service("ovhFileStorageProvider")
@Slf4j
@Profile("!mockOvh")
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

    @Override
    public ObjectStorageProvider getProvider() {
        return ObjectStorageProvider.OVH;
    }

    @Override
    @Async
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
            try {
                Cipher aes;
                if (key.getVersion() == 1) {
                    byte[] iv = DigestUtils.md5(path); // arbitrary set the filename to build IV
                    GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                    aes = Cipher.getInstance("AES/GCM/NoPadding");
                    aes.init(Cipher.DECRYPT_MODE, key, gcmParamSpec);
                } else {
                    throw new UnsupportedKeyException("Unsupported key version " + key.getVersion());
                }
                in = new CipherInputStream(in, aes);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return in;
    }

    @Override
    public void upload(String ovhPath, InputStream inputStream, EncryptionKey key, String contentType) throws RetryableOperationException, IOException {
        if (key != null) {
            if (key.getVersion() != 1){
                throw new UnsupportedKeyException("Unsupported key version " + key.getVersion());
            }
            try {
                byte[] iv = DigestUtils.md5(ovhPath);
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
            eTag = getClient().objectStorage().objects().put(ovhContainerName, ovhPath, payload);
            SwiftObject metaData = getClient().objectStorage().objects().get(ovhContainerName, ovhPath);
            if (metaData.getSizeInBytes() <= 0) {
                throw new IOException("File size is null - upload failed for: " + ovhPath);
            }

        } catch (AuthenticationException e) {
            log.error("ObjectStorage authentication failed.", e);
            eTag = authenticate().objectStorage().objects().put(ovhContainerName, ovhPath, payload);
        } catch (OvhConnectionFailedException e) {
            throw new RetryableOperationException("Ovh Connection Failed", e);
        }
        if (StringUtils.isEmpty(eTag)) {
            throw new IOException("ETag is empty - upload failed!" + ovhPath);
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

}
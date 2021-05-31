package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.FileCannotUploadedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class OvhService {
    @Value("${ovh.project.domain}")
    private String ovhProjectDomain;
    @Value("${ovh.auth.url}")
    private String ovhAuthUrl;
    @Value("${ovh.username}")
    private String ovhUsername;
    @Value("${ovh.password}")
    private String ovhPassword;
    @Value("${ovh.project.name}")
    private String ovhProjectName;
    @Value("${ovh.region}")
    private String ovhRegion;
    @Value("${ovh.container}")
    private String ovhContainerName;

    private OSClient.OSClientV3 connect() {
        Identifier domainIdentifier = Identifier.byId(ovhProjectDomain);

        try {
            OSClient.OSClientV3 os = OSFactory.builderV3()
                    .endpoint(ovhAuthUrl)
                    .credentials(ovhUsername, ovhPassword, domainIdentifier)
                    .scopeToProject(Identifier.byName(ovhProjectName), Identifier.byName(ovhProjectDomain))
                    .authenticate();
            os.useRegion(ovhRegion);
            return os;
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            return null;
        }
    }

    public String uploadFile(MultipartFile file) {
        String name = UUID.randomUUID().toString() + "." + Objects.requireNonNull(FilenameUtils.getExtension(file.getOriginalFilename())).toLowerCase(Locale.ROOT);
        try {
            upload(name, file.getInputStream());
        } catch (IOException e) {
            throw new FileCannotUploadedException();
        }
        return name;
    }

    void upload(String ovhPath, InputStream inputStream) {
        OSClient.OSClientV3 os = connect();
        if (os != null) {
            os.objectStorage().objects().put(ovhContainerName, ovhPath, Payloads.create(inputStream));
        }
    }

    public SwiftObject get(String name) {
        OSClient.OSClientV3 os = connect();
        return os != null ? os.objectStorage().objects().get(ovhContainerName, name) : null;
    }

    @Async
    public void delete(List<String> name) {
        name.forEach(this::delete);
    }

    @Async
    public void delete(String name) {
        OSClient.OSClientV3 os = connect();
        if (os != null) {
            os.objectStorage().objects().delete(ovhContainerName, name);
        }
    }

    public List<? extends SwiftObject> listObjectContainer() {
        OSClient.OSClientV3 os = connect();
        return os != null ? os.objectStorage().objects().list(ovhContainerName) : null;
    }
}

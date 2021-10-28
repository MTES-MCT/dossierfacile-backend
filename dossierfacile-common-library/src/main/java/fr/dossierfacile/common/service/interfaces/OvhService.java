package fr.dossierfacile.common.service.interfaces;

import org.openstack4j.model.storage.object.SwiftObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface OvhService {
    void delete(String name);

    void delete(List<String> name);

    void deleteAllFiles(String path);

    SwiftObject get(String name);

    List<? extends SwiftObject> getListObject(String folderName);

    List<? extends SwiftObject> listObjectContainer();

    void upload(String ovhPath, InputStream inputStream);

    String uploadFile(MultipartFile file);
}

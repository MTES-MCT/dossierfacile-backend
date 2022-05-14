package fr.dossierfacile.garbagecollector.service.interfaces;

import java.util.List;
import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.model.storage.object.SwiftObject;

public interface OvhService {
    void delete(String name);

    void delete(List<String> name);

    SwiftObject get(String name);

    ObjectStorageObjectService getObjectStorage();
}

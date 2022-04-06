package com.object.checker.service.interfaces;

import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.model.storage.object.SwiftObject;

import java.util.List;

public interface OvhService {
    void delete(String name);

    void delete(List<String> name);

    SwiftObject get(String name);

    ObjectStorageObjectService getObjectStorage();
}

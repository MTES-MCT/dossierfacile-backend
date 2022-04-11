package fr.dossierfacile.garbagecollector.service.interfaces;

import fr.dossierfacile.garbagecollector.model.object.Object;

import java.util.List;

public interface ObjectService {
    void register(Object t);

    List<Object> getAll();

    long getObjectListToDeleteTrue();

    List<Object> getBatchObjectsToDelete();

    void deleteList(List<Object> objectList);

    List<Object> getAllObjectInTrue();

    void updateAllToDeleteObjects();

    void deleteObjectByPath(String path);

    Object findObjectByPath(String path);
}

package fr.dossierfacile.garbagecollector.service.interfaces;

import fr.dossierfacile.garbagecollector.model.object.Object;
import java.util.List;

public interface ObjectService {

    List<Object> getBatchObjectsForDeletion(Integer limit);

    List<Object> getAllObjectsForDeletion();

//    void updateAllToDeleteObjects();

    long countAllObjectsScanned();

    long countAllObjectsForDeletion();

    void deleteList(List<Object> objectList);

    void deleteObjectByPath(String path);

    Object findObjectByPath(String path);
}

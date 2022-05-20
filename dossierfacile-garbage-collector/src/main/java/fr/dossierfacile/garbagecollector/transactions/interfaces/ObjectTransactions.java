package fr.dossierfacile.garbagecollector.transactions.interfaces;

import fr.dossierfacile.garbagecollector.model.object.Object;
import java.util.List;

public interface ObjectTransactions {
    void saveObjectIfDoesntExist(String nameFile);
    void deleteAllObjects();
    void deleteListObjects(List<Object> objectList);
    void deleteObjectsMayorThan(String path);
}

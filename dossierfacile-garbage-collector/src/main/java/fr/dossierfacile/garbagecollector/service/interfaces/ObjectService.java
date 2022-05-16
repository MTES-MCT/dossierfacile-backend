package fr.dossierfacile.garbagecollector.service.interfaces;

import fr.dossierfacile.garbagecollector.model.object.Object;
import java.util.List;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;

public interface ObjectService {

    List<Object> getBatchObjectsForDeletion(Integer limit);

    DataTablesOutput<Object> getAllObjectsForDeletion(DataTablesInput input);

    long countAllObjectsScanned();

    long countAllObjectsForDeletion();

    void deleteList(List<Object> objectList);

    void deleteObjectByPath(String path);
}

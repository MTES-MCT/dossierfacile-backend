package fr.dossierfacile.garbagecollector.service.interfaces;

import fr.dossierfacile.garbagecollector.model.StoredObject;

import javax.annotation.Nullable;
import java.util.List;

public interface StorageProviderService {

    List<StoredObject> getObjectsStartingAtMarker(@Nullable String marker, int numberOfObjects);

    void delete(StoredObject object);

}

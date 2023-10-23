package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.garbagecollector.model.garbagecollection.GarbageCollectionDetails;
import fr.dossierfacile.garbagecollector.model.StoredObject;
import fr.dossierfacile.garbagecollector.repo.garbagecollection.GarbageCollectionDetailsRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.StorageProviderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@AllArgsConstructor
public class ScheduledGarbageCollectionService {

    private final GarbageCollectionDetailsRepository garbageCollectionDetailsRepository;
    private final StorageFileRepository storageFileRepository;
    private final Map<ObjectStorageProvider, StorageProviderService> storageProviderServices;

    private final int numberOfObjectsToCheckByIteration;

    @Scheduled(fixedDelayString = "${garbage-collection.seconds-between-iterations:60}", timeUnit = SECONDS)
    void cleanGarbage() {
        log.info("Starting garbage collection iteration");
        storageProviderServices.keySet().forEach(this::cleanGarbageOn);
        log.info("Finished garbage collection iteration");
    }

    private void cleanGarbageOn(ObjectStorageProvider provider) {
        GarbageCollectionDetails garbageCollectionDetails = getGarbageCollectionDetails(provider);

        List<StoredObject> objects = retrieveObjectsFromStorage(provider, garbageCollectionDetails);

        if (!objects.isEmpty()) {
            int deletedObjects = deleteOrphanObjects(objects, garbageCollectionDetails);
            updateDetails(garbageCollectionDetails, objects, deletedObjects);
        }
    }

    private GarbageCollectionDetails getGarbageCollectionDetails(ObjectStorageProvider provider) {
        Optional<GarbageCollectionDetails> details = garbageCollectionDetailsRepository.findById(provider);
        if (details.isPresent()) {
            return details.get();
        }
        GarbageCollectionDetails created = GarbageCollectionDetails.builder().provider(provider).build();
        garbageCollectionDetailsRepository.save(created);
        return created;
    }

    private List<StoredObject> retrieveObjectsFromStorage(ObjectStorageProvider provider, GarbageCollectionDetails garbageCollectionDetails) {
        String currentMarker = garbageCollectionDetails.getCurrentMarker();
        List<StoredObject> objects = getProviderService(garbageCollectionDetails)
                .getObjectsStartingAtMarker(currentMarker, numberOfObjectsToCheckByIteration);
        log.info("Retrieved {} objects from storage {} to search in database", objects.size(), provider);
        return objects;
    }

    private int deleteOrphanObjects(List<StoredObject> objects, GarbageCollectionDetails garbageCollectionDetails) {
        List<String> existingStorageFilePaths = findExistingStorageFilePaths(objects);

        List<StoredObject> objectsToDelete = objects.stream()
                .filter(object -> !existingStorageFilePaths.contains(object.getName()))
                .toList();

        log.info("Found {} objects out of {} to delete", objectsToDelete.size(), objects.size());

        StorageProviderService providerService = getProviderService(garbageCollectionDetails);
        objectsToDelete.forEach(object -> {
            log.info("Deleting object {} from {} storage", object.getName(), garbageCollectionDetails.getProvider());
            providerService.delete(object);
        });

        return objectsToDelete.size();
    }

    private StorageProviderService getProviderService(GarbageCollectionDetails garbageCollectionDetails) {
        return storageProviderServices.get(garbageCollectionDetails.getProvider());
    }

    private List<String> findExistingStorageFilePaths(List<StoredObject> objects) {
        List<String> paths = objects.stream().map(StoredObject::getName).toList();
        return storageFileRepository.findExistingPathsIn(paths);
    }

    private void updateDetails(GarbageCollectionDetails garbageCollectionDetails, List<StoredObject> checkedObjects, int deletedObjects) {
        String newMarker = checkedObjects.get(checkedObjects.size() - 1).getName();
        log.info("Setting marker to {} for next iteration", newMarker);
        garbageCollectionDetails.setCurrentMarker(newMarker);
        garbageCollectionDetails.setDeletedObjects(garbageCollectionDetails.getDeletedObjects() + deletedObjects);
        garbageCollectionDetailsRepository.save(garbageCollectionDetails);
    }

}

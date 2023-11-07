package fr.dossierfacile.scheduler.service;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import fr.dossierfacile.scheduler.model.StoredObject;
import fr.dossierfacile.scheduler.model.garbagecollection.GarbageCollectionDetails;
import fr.dossierfacile.scheduler.repo.garbagecollection.GarbageCollectionDetailsRepository;
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
    private final Map<ObjectStorageProvider, FileStorageProviderService> storageProviderServices;

    private final int numberOfObjectsToCheckByIteration;

    @Scheduled(fixedDelayString = "${garbage-collection.seconds-between-iterations:60}", timeUnit = SECONDS)
    void cleanGarbage() {
        log.info("Starting garbage collection iteration");
        storageProviderServices.keySet().forEach(this::cleanGarbageOn);
        log.info("Finished garbage collection iteration");
    }

    private void cleanGarbageOn(ObjectStorageProvider provider) {
        GarbageCollectionDetails garbageCollectionDetails = getGarbageCollectionDetails(provider);

        List<StoredObject> objects = retrieveObjectsFromStorage(garbageCollectionDetails);

        if (objects.isEmpty()) {
            log.info("All objects in {} have been analyzed, resetting marker", provider);
            resetMarker(garbageCollectionDetails);
        } else {
            int deletedObjects = findAndDeleteOrphanObjects(objects, garbageCollectionDetails);
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

    private List<StoredObject> retrieveObjectsFromStorage(GarbageCollectionDetails garbageCollectionDetails) {
        String currentMarker = garbageCollectionDetails.getCurrentMarker();
        List<StoredObject> objects = getProviderService(garbageCollectionDetails)
                .listObjectNames(currentMarker, numberOfObjectsToCheckByIteration)
                .stream().map(StoredObject::new).toList();
        log.info("Retrieved {} objects from storage {} to search in database",
                objects.size(), garbageCollectionDetails.getProvider());
        return objects;
    }

    private int findAndDeleteOrphanObjects(List<StoredObject> objectsToSearch, GarbageCollectionDetails garbageCollectionDetails) {
        List<String> existingStorageFilePaths = findExistingStorageFilePaths(objectsToSearch);

        List<StoredObject> objectsToDelete = objectsToSearch.stream()
                .filter(object -> !existingStorageFilePaths.contains(object.getName()))
                .toList();

        log.info("Found {} objects out of {} to delete", objectsToDelete.size(), objectsToSearch.size());

        deleteObjects(objectsToDelete, getProviderService(garbageCollectionDetails));

        return objectsToDelete.size();
    }

    private void deleteObjects(List<StoredObject> objectsToDelete, FileStorageProviderService providerService) {
        objectsToDelete.forEach(object -> {
            String name = object.getName();
            log.info("Deleting object {} from {} storage", name, providerService.getProvider());
            providerService.delete(name);
        });
    }

    private FileStorageProviderService getProviderService(GarbageCollectionDetails garbageCollectionDetails) {
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

    private void resetMarker(GarbageCollectionDetails garbageCollectionDetails) {
        garbageCollectionDetails.setCurrentMarker(null);
        garbageCollectionDetailsRepository.save(garbageCollectionDetails);
    }

}

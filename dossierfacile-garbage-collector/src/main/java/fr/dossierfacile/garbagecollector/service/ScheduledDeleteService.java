package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.common.entity.StorageFileToDelete;
import fr.dossierfacile.common.repository.StorageFileToDeleteRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageToDeleteService;
import fr.dossierfacile.garbagecollector.model.object.Object;
import fr.dossierfacile.garbagecollector.service.interfaces.ObjectService;
import fr.dossierfacile.garbagecollector.service.interfaces.OvhService;
import fr.dossierfacile.garbagecollector.transactions.interfaces.ObjectTransactions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledDeleteService {

    private final StorageFileToDeleteRepository storageFileToDeleteRepository;
    private final FileStorageToDeleteService fileStorageToDeleteService;
    private static final Integer LIMIT_OBJECTS_TO_DELETE = 500;
    private final ObjectService objectService;
    private final ObjectTransactions objectTransactions;
    private final OvhService ovhService;
    private final AtomicBoolean isActive = new AtomicBoolean(false);

    public boolean isActive() {
        return isActive.get();
    }

    public void setIsActive(boolean value) {
        this.isActive.set(value);
    }

    /**
     * Every 5 seconds will delete a maximum of 500 elements
     */
    @Scheduled(fixedDelay = 5000)
    public void scheduleFixedDelayTask() {
        if (!isActive.get()) return;
        //check if there is connection to ovh
        if (ovhService.getObjectStorage() == null) {
            log.warn("No connection to OVH " + "\n");
            return;
        }
        List<Object> objectList = objectService.getBatchObjectsForDeletion(LIMIT_OBJECTS_TO_DELETE);
        //check if there are data to process
        if (objectList.isEmpty()) {
            log.warn("No objects to remove found." + "\n");
            this.isActive.set(false);
            return;
        }
        List<String> allPaths = objectList.stream().map(Object::getPath).collect(Collectors.toList());
        ovhService.delete(allPaths);
        objectTransactions.deleteListObjects(objectList);
        System.out.println("Deleted files: " + objectList.size() + "\n");
    }

    @Scheduled(fixedDelay = 9000)
    public void deleteFileInProviderTask() {
        List<StorageFileToDelete> storageFileToDeleteList = storageFileToDeleteRepository.findAll();
        for (StorageFileToDelete storageFileToDelete : storageFileToDeleteList) {
            fileStorageToDeleteService.delete(storageFileToDelete);
        }
    }
}

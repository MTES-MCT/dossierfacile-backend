package fr.dossierfacile.garbagecollector.service;

import com.google.common.base.Preconditions;
import fr.dossierfacile.garbagecollector.model.marker.Marker;
import fr.dossierfacile.garbagecollector.model.object.Object;
import fr.dossierfacile.garbagecollector.repo.file.FileRepository;
import fr.dossierfacile.garbagecollector.repo.marker.MarkerRepository;
import fr.dossierfacile.garbagecollector.repo.object.ObjectRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.MarkerService;
import fr.dossierfacile.garbagecollector.service.interfaces.OvhService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarkerServiceImpl implements MarkerService {

    private static final int PAGE_SIZE = 10;

    private final OvhService ovhService;
    private final MarkerRepository markerRepository;
    private final ObjectRepository objectRepository;
    private final FileRepository fileRepository;

    private boolean isRunning = false;

    @Value("${ovh.container:default}")
    private String ovhContainerName;

    @Override
    public boolean toggleScanner() {
        isRunning = !isRunning;
        return isRunning;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    @Async
    public void startScanner() {
        if (!isRunning) {
            return;
        }
        log.info("Starting/Resuming scanner ...");
        log.info("Connecting to OVH ...");
        final ObjectStorageObjectService objService = ovhService.getObjectStorage();
        int totalObjectsInOvh = objService.list(ovhContainerName).size();
        log.info("Total objects in OVH : " + totalObjectsInOvh);

        try {
            final ObjectListOptions listOptions = initialize();
            log.info("Reading objects from OVH: [STARTED]");
            List<? extends SwiftObject> objects;
            do {
                if (!isRunning) {
                    break;
                }
                //get list of object from OVH
                objects = objService.list(ovhContainerName, listOptions);
                //save marker
                if (!objects.isEmpty()) {
                    String nameLastElement = objects.get(objects.size() - 1).getName();
                    listOptions.marker(nameLastElement);
                    Marker marker = new Marker();
                    marker.setPath(nameLastElement);
                    markerRepository.save(marker);

                    //copy the names of objects from OVH to DB
                    for (final SwiftObject swiftObject : objects) {
                        if (!isRunning) {
                            break;
                        }
                        String nameFile = swiftObject.getName();
                        Object object = new Object();
                        // If the [File] doesn't exists in the [Database] then it will be marked with [true] to delete
                        object.setToDelete(!fileRepository.existsObject(nameFile));
                        object.setPath(nameFile);
                        objectRepository.save(object);
                    }
                    if (!isRunning) {
                        break;
                    }
                    long totalObjectsRead = objectRepository.count();
                    log.info("Objects read in the iteration [" + objects.size() + "]");
                    log.info("Total objects read : " + totalObjectsRead + " from " + totalObjectsInOvh);
                    log.info("Remaining objects : " + (totalObjectsInOvh - totalObjectsRead));
                }
            } while (objects.size() == PAGE_SIZE);

        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
        }
        log.info("Disconnecting from OVH ...");
        isRunning = false;
    }

    @Override
    public void setRunningToFalse() {
        synchronized (this) {
            isRunning = false;
            log.info("Stopping scanner...");
            try {
                this.wait(10000);
                log.info("Waiting 10 seconds...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setRunningToTrue() {
        isRunning = true;
    }

    @Override
    @Transactional
    public void cleanDatabaseOfScanner() {
        markerRepository.deleteAll();
        objectRepository.deleteAll();
    }

    private ObjectListOptions initialize() {
        final ObjectListOptions listOptions = ObjectListOptions.create().limit(PAGE_SIZE);
        if (markerRepository.count() == 0) {
            return listOptions;
        } else if (markerRepository.count() == 1) {
            markerRepository.deleteAll();
            objectRepository.deleteAll();
        } else {
            // markerRepository.count() >= 2
            Marker penultimateMarker = markerRepository.findAll().get((int) markerRepository.count() - 2);
            Marker lastMarker = markerRepository.findAll().get((int) markerRepository.count() - 1);
            String markerBeforeLastOne = penultimateMarker.getPath();
            listOptions.marker(markerBeforeLastOne);

            //delete last marker to ensure checking again all elements between penultimate and last elements were processed
            markerRepository.delete(lastMarker);
            Object object = objectRepository.findObjectByPath(penultimateMarker.getPath());
            objectRepository.deleteObjectsMayorThan(Preconditions.checkNotNull(object, "object can not be null").getId());
        }
        return listOptions;
    }
}

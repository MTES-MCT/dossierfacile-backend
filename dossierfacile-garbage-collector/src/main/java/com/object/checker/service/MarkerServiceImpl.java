package com.object.checker.service;

import com.object.checker.model.marker.Marker;
import com.object.checker.model.object.Object;
import com.object.checker.repo.file.fileRepo;
import com.object.checker.repo.marker.markerRepo;
import com.object.checker.repo.object.objectRepo;
import com.object.checker.service.interfaces.MarkerService;
import com.object.checker.service.interfaces.ObjectService;
import com.object.checker.service.interfaces.OvhService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarkerServiceImpl implements MarkerService {

    private final ObjectService objectService;
    private final OvhService ovhService;
    private final markerRepo markerRepo;
    private final objectRepo objectRepo;
    private final fileRepo fileRepo;
    private boolean isRunning = false;
    private AtomicInteger readCount = new AtomicInteger(0);

    private static final int PAGE_SIZE = 1000;

    @Value("${ovh.container:default}")
    private String ovhContainerName;

    public void savaData(Marker marker) {
        markerRepo.save(marker);
    }

    public String getLastMark() {
        return markerRepo.findAll().get(markerRepo.findAll().size() - 1).getPath();
    }

    public String getLastMark2() {
        return markerRepo.findAll().get(markerRepo.findAll().size() - 2).getPath();
    }

    public int getReadCount(){
        return readCount.get();
    }

    public boolean isStarted() {
        return markerRepo.findAll().size() > 0;
    }

    private ObjectListOptions initialize(){
        final ObjectListOptions listOptions = ObjectListOptions.create().limit(PAGE_SIZE);
        if (!isStarted()) return listOptions;
        //re-start search from the last marker
        String last;
        if (markerRepo.findAll().size() >= 2) {
            listOptions.marker(getLastMark2());
            last = getLastMark2();
            markerRepo.delete(markerRepo.findByPath(getLastMark()));
        } else {
            listOptions.marker(getLastMark());
            last = getLastMark();
        }

        //delete all object after the previous marker

        Object object = objectRepo.findObjectByPath(last);
        if (object != null) {
            objectRepo.deleteObjectsMayorThan(object.getId());
        }
        return listOptions;
    }

    @Async
    public void getObjectFromOvhAndProcess() {
        if (this.isRunning) return;
        String anim = "|/-\\";
        int index = 0;
        this.isRunning = true;
        System.out.println("Connecting to OVH ...\n");
        final ObjectStorageObjectService objService = ovhService.getObjectStorage();
        readCount.set(0);
        try {
            final ObjectListOptions listOptions = initialize();
            System.out.println("Reading objects from OVH: [STARTED]" + "\n");
            List<? extends SwiftObject> objects;
            do {
                //get list of object from OVH
                objects = objService.list(ovhContainerName, listOptions);
                //save marker
                if (!objects.isEmpty()) {
                    listOptions.marker(objects.get(objects.size() - 1).getName());
                    Marker marker = new Marker();
                    marker.setPath(objects.get(objects.size() - 1).getName());
                    savaData(marker);
                }
                var toDeleteCount = 0;
                //copy and process object from OVH to DB
                for (final SwiftObject swiftObject : objects) {
                    index++;
                    readCount.set(readCount.get() + 1);
                    String data = "\r" + anim.charAt(index % anim.length()) + " " + index;
                    if (!fileRepo.existsObject(swiftObject.getName())) { /// check if path in object not match with path in BD-DOSSIER
                        Object object = new Object();
                        object.setTo_delete(true);
                        object.setPath(swiftObject.getName());
                        objectService.register(object);
                        toDeleteCount++;
                    }
                }
                System.out.println("Total objects Read/ToDelete: " + objects.size() + "/"+ toDeleteCount + "\n");
            } while (objects.size() >= PAGE_SIZE);

        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
        }
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

}

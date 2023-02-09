package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.garbagecollector.model.marker.Marker;
import fr.dossierfacile.garbagecollector.model.object.Object;
import fr.dossierfacile.garbagecollector.repo.file.FileRepository;
import fr.dossierfacile.garbagecollector.repo.marker.MarkerRepository;
import fr.dossierfacile.garbagecollector.repo.object.ObjectRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.MarkerService;
import fr.dossierfacile.garbagecollector.service.interfaces.OvhService;
import fr.dossierfacile.garbagecollector.transactions.interfaces.MarkerTransactions;
import fr.dossierfacile.garbagecollector.transactions.interfaces.ObjectTransactions;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarkerServiceImpl implements MarkerService {

    private static final int PAGE_SIZE = 100;
    public static final String MARKER_FOR_DELETION = "GARBAGE_";

    private final OvhService ovhService;
    private final MarkerTransactions markerTransactions;
    private final ObjectTransactions objectTransactions;
    private final MarkerRepository markerRepository;
    private final ObjectRepository objectRepository;
    private final FileRepository fileRepository;

    private boolean isRunning = false;
    private boolean isCanceled = false;

    @Value("${ovh.container:default}")
    private String ovhContainerName;

    @Override
    public boolean toggleScanner() {
        boolean toggleToStart = false;
        if (isRunning) {
            //the startScanner() method WILL STOP running (isRunning=false) after reading isCanceled = true;
            isCanceled = true;
        } else {
            //the startScanner() method WILL START running (isRunning=true) after the controller launch it
            isCanceled = false;
            isRunning = true;
            toggleToStart = true;
        }
        return toggleToStart;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public boolean stoppingScanner() {
        return isRunning && isCanceled;
    }

    @Override
    public void stopScanner() {
        if (isRunning) {
            isCanceled = true;
        }
    }

    @Override
    public void setRunningToTrue() {
        isRunning = true;
    }

    @Override
    @Async
    public void startScanner() {
        if (isCanceled) {
            isRunning = false;
            isCanceled = false;
            return;
        }
        log.info("Starting/Resuming scanner ...");
        log.info("Connecting to OVH ...\n");

        final ObjectStorageObjectService objService = ovhService.getObjectStorage();

        int iterationNumber = 1;
        try {
            final ObjectListOptions listOptions = initialize();

            int totalObjectsRead = (int) objectRepository.count();

            log.info("\n----------------------------------------" +
                    "\nTotal objects read : " + totalObjectsRead +
                    "\n----------------------------------------");

            List<? extends SwiftObject> objects;
            do {
                long iterationElapsedTime = System.currentTimeMillis();
                if (isCanceled) {
                    break;
                }
                //get list of object from OVH
                long start1 = System.currentTimeMillis();
                objects = objService.list(ovhContainerName, listOptions);
                long end1 = System.currentTimeMillis();
                System.out.println("Elapsed Time for get object list in mili seconds: " + (end1 - start1));
                //save marker
                if (!objects.isEmpty()) {
                    String nameLastElement = objects.get(objects.size() - 1).getName();
                    listOptions.marker(nameLastElement);

                    markerTransactions.saveMarkerIfNotYetSaved(nameLastElement);

                    start1 = System.currentTimeMillis();
                    List<Object> existingObjects = objectRepository.findObjectsByPaths(objects.stream().map(SwiftObject::getName).collect(Collectors.toList()));
                    objects = objects.stream().filter(o -> existingObjects.stream().filter(e -> e.getPath().equals(o.getName())).findAny().isEmpty()).collect(Collectors.toList());

                    end1 = System.currentTimeMillis();
                    System.out.println("Elapsed Time for object filtering in mili seconds: " + (end1 - start1));

                    start1 = System.currentTimeMillis();
                    List<String> names = objects.stream()
                            .map(SwiftObject::getName)
                            .filter(name -> !name.startsWith(MARKER_FOR_DELETION))
                            .collect(Collectors.toList());
                    List<String> matchingFilesInDB = fileRepository.existingFiles(names);
                    end1 = System.currentTimeMillis();
                    System.out.println("Elapsed Time for get existing files in mili seconds: " + (end1 - start1));

                    objects.parallelStream().forEach(swiftObject -> {
                        if (isCanceled) {
                            return;
                        }
                        String nameFile = swiftObject.getName();
                        if (!nameFile.startsWith(MARKER_FOR_DELETION)) {
                            nameFile = renameFileIfNotInDatabase(nameFile, matchingFilesInDB);
                        }
                        objectTransactions.saveObject(nameFile);
                    });

                    //copy the names of objects from OVH to DB
                    if (isCanceled) {
                        break;
                    }
                    totalObjectsRead = (int) objectRepository.count();
                    iterationElapsedTime = System.currentTimeMillis() - iterationElapsedTime;
                    String textIteration = "------ Iteration [" + iterationNumber++ + "] ------- " + iterationElapsedTime / 1000 + "sec. --";
                    log.info("\n\n" + textIteration +
                            "\nTotal objects read : " + totalObjectsRead +
                            "\n" + "-".repeat(textIteration.length()) + "\n");
                } else {
                    log.info("SCANNING FINISHED\n");
                }
            } while (!objects.isEmpty());

        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
        }
        log.info("Disconnecting from OVH ...\n");
        isRunning = false;
        isCanceled = false;
    }

    private ObjectListOptions initialize() {
        final ObjectListOptions listOptions = ObjectListOptions.create().limit(PAGE_SIZE);
        if (markerRepository.count() == 0) {
            return listOptions;
        } else if (markerRepository.count() == 1) {
            markerTransactions.deleteAllMarkers();
            objectTransactions.deleteAllObjects();
        } else {
            // markerRepository.count() >= 2
            List<Marker> lastTwoMarkers = markerRepository.lastTwoMarkers();
            Marker lastMarker = lastTwoMarkers.get(0);
            Marker penultimateMarker = lastTwoMarkers.get(1);

            String markerBeforeLastOne = penultimateMarker.getPath();
            listOptions.marker(markerBeforeLastOne);

            //delete last marker to ensure checking again all elements between penultimate and last elements were processed
            markerTransactions.deleteMarker(lastMarker);
            long number1 = objectRepository.countAllByPath(penultimateMarker.getPath());
            long number2 = objectRepository.countAllByPath(MARKER_FOR_DELETION + penultimateMarker.getPath());
            Assert.isTrue(number1 + number2 == 1, "There is no exactly 1 marker with name [" + penultimateMarker.getPath() + "] or [" + MARKER_FOR_DELETION + penultimateMarker.getPath() + "] in the objects saved");
            objectTransactions.deleteObjectsMayorThan(penultimateMarker.getPath());
        }
        return listOptions;
    }

    private String renameFileIfNotInDatabase(String nameFile, List<String> names) {
        if (!names.contains(nameFile)) {
            String oldName = nameFile;
            nameFile = MARKER_FOR_DELETION + nameFile;
            ovhService.renameFile(oldName, nameFile);
        }
        return nameFile;
    }
}

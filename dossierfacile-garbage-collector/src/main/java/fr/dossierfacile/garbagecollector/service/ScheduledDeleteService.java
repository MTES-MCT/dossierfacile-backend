package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.garbagecollector.model.object.Object;
import fr.dossierfacile.garbagecollector.service.interfaces.ObjectService;
import fr.dossierfacile.garbagecollector.service.interfaces.OvhService;
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
public class ScheduledDeleteService  {

    private final ObjectService objectService;
    private final OvhService ovhService;
    private AtomicBoolean isActive = new AtomicBoolean(false);
    public boolean IsActive(){
        return isActive.get();
    }

    public void setIsActive(boolean value) {
        this.isActive.set(value);
    }

    @Scheduled(fixedDelay  = 5000)
    public void scheduleFixedDelayTask()  {
        if (!isActive.get()) return;
        //check if there are connection to ovh
        if (ovhService.getObjectStorage() == null)
        {
            log.warn("No connection to OVH " + "\n");
            return;
        }
        List<Object> objectList = objectService.getBatchObjectsToDelete();
        //check if there are data to process
        if (objectList.isEmpty())
        {
            //log.warn("No objects to remove found." + "\n");
            return;
        }
        List<String> allPaths = objectList.stream().map(Object::getPath).collect(Collectors.toList());
        ovhService.delete(allPaths);
        objectService.deleteList(objectList);
        System.out.println("Deleted files: " + objectList.size() + "\n");
    }
}

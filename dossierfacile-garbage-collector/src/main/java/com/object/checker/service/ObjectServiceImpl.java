package com.object.checker.service;

import com.object.checker.model.object.Object;
import com.object.checker.repo.object.objectRepo;
import com.object.checker.service.interfaces.ObjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ObjectServiceImpl implements ObjectService {

    private final objectRepo objectRepo;

    public void register(Object t) {
        objectRepo.save(t);
    }

    public List<Object> getAll() {
        return objectRepo.findAll();
    }

    public long getObjectListToDeleteTrue() {
        return objectRepo.countAllObjectForDelete();
    }

    public List<Object> getBatchObjectsToDelete() {
        return objectRepo.getBatchObjectToDeleteInTrue();
    }

    public void deleteList(List<Object> objectList) {
        objectRepo.deleteAll(objectList);
    }

    public List<Object> getAllObjectInTrue() {
        return objectRepo.getAllObjectsInTrue();
    }

    @Async
    public void updateAllToDeleteObjects() {
        System.out.println(" marking object for delete [STARTED]" + "\n");
        for (Object pathOvh : getAll()) {
            if (!pathOvh.isTo_delete()) {
                pathOvh.setTo_delete(true);
                register(pathOvh);
            }
        }
        System.out.println(" marking object for delete [FINISHED]" + " total to_delete: [" + getObjectListToDeleteTrue() + "] " + "\n");
    }


    @Transactional
    public void deleteObjectByPath(String path) {
        objectRepo.deleteObjectByPath(path);
    }

    public Object findObjectByPath(String path) {
        return objectRepo.findObjectByPath(path);
    }

}

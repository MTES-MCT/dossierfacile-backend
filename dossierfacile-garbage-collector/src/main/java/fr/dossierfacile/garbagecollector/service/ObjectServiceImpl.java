package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.garbagecollector.model.object.Object;
import fr.dossierfacile.garbagecollector.repo.object.ObjectRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.ObjectService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ObjectServiceImpl implements ObjectService {

    private final ObjectRepository objectRepository;

    @Override
    public List<Object> getBatchObjectsForDeletion(Integer limit) {
        return objectRepository.getBatchObjectsForDeletion(limit);
    }

    @Override
    public List<Object> getAllObjectsForDeletion() {
        return objectRepository.getAllObjectsForDeletion();
    }

//    @Override
//    @Async
//    public void updateAllToDeleteObjects() {
//        System.out.println(" marking object for delete [STARTED]" + "\n");
//        for (Object pathOvh : objectRepo.findAll()) {
//            if (pathOvh.isToDelete()) {
//                pathOvh.setToDelete(true);
//                objectRepo.save(pathOvh);
//            }
//        }
//        System.out.println(" marking object for delete [FINISHED]" + " total to_delete: [" + getObjectListToDeleteTrue() + "] " + "\n");
//    }

    @Override
    public long countAllObjectsScanned() {
        return objectRepository.count();
    }

    @Override
    public long countAllObjectsForDeletion() {
        return objectRepository.countAllObjectsForDeletion();
    }

    @Override
    public void deleteList(List<Object> objectList) {
        objectRepository.deleteAll(objectList);
    }

    @Override
    public void deleteObjectByPath(String path) {
        objectRepository.deleteObjectByPath(path);
    }

    @Override
    public Object findObjectByPath(String path) {
        return objectRepository.findObjectByPath(path);
    }
}

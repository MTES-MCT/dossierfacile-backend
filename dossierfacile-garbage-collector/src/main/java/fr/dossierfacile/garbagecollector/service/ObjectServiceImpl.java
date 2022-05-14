package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.garbagecollector.model.object.Object;
import fr.dossierfacile.garbagecollector.repo.object.ObjectRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.ObjectService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public DataTablesOutput<Object> getAllObjectsForDeletion(DataTablesInput input) {
        Specification<Object> specification = (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("toDelete"));
        return objectRepository.findAll(input, specification);
    }

    @Override
    public long countAllObjectsScanned() {
        return objectRepository.count();
    }

    @Override
    public long countAllObjectsForDeletion() {
        return objectRepository.countByToDeleteIsTrue();
    }

    @Override
    public void deleteList(List<Object> objectList) {
        objectRepository.deleteAll(objectList);
    }

    @Override
    @Transactional
    public void deleteObjectByPath(String path) {
        objectRepository.deleteByPath(path);
    }
}

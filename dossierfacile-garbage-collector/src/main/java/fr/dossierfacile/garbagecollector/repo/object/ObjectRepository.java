package fr.dossierfacile.garbagecollector.repo.object;

import fr.dossierfacile.garbagecollector.model.object.Object;
import java.util.List;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ObjectRepository extends DataTablesRepository<Object, Long> {

    Object findObjectByPath(String path);

    void deleteByPath(String path);

    long countByToDeleteIsTrue();

    @Query(value = "SELECT * FROM object where to_delete = true ORDER BY id LIMIT :limit", nativeQuery = true)
    List<Object> getBatchObjectsForDeletion(@Param("limit") Integer limit);

    @Modifying
    @Transactional
    @Query(value = "delete from object where id > (select id from object where path = :path)", nativeQuery = true)
    void deleteObjectsMayorThan(@Param("path") String path);

    long countAllByPath(String path);
}

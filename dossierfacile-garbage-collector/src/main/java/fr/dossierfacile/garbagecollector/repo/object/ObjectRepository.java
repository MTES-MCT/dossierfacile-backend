package fr.dossierfacile.garbagecollector.repo.object;

import fr.dossierfacile.garbagecollector.model.object.Object;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ObjectRepository extends JpaRepository<Object, Long> {

    Object findObjectByPath(String path);

    void deleteObjectByPath(String path);

    @Query(value = "SELECT count(ob) from Object ob where ob.toDelete = true")
    long countAllObjectsForDeletion();

    @Query(value = "SELECT * FROM object where to_delete = true ORDER BY id LIMIT :limit",nativeQuery = true)
    List<Object> getBatchObjectsForDeletion(@Param("limit") Integer limit);

    @Query(value = "SELECT * FROM object where to_delete = true",nativeQuery = true)
    List<Object> getAllObjectsForDeletion();

    @Modifying
    @Transactional
    @Query(value="delete from object where id > :id_obj ",nativeQuery = true)
    void deleteObjectsMayorThan(@Param("id_obj") Long id_obj);
}

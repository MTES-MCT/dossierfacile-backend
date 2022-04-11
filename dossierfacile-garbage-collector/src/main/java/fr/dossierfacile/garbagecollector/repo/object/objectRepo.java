package fr.dossierfacile.garbagecollector.repo.object;

import fr.dossierfacile.garbagecollector.model.object.Object;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface objectRepo extends JpaRepository<Object, Long> {

    Object findObjectByPath(String path);

    void deleteObjectByPath(String path);

    @Query(value = "SELECT count(ob) from Object ob where ob.to_delete = true")
    long countAllObjectForDelete();

    @Query(value = "SELECT * FROM object where to_delete = true ORDER BY id LIMIT 500",nativeQuery = true)
    List<Object> getBatchObjectToDeleteInTrue();

    @Query(value= "SELECT * FROM object where to_delete = true",nativeQuery = true)
    List<Object> getAllObjectsInTrue();

    @Modifying
    @Transactional
    @Query(value="delete from object where id > :id_obj ",nativeQuery = true)
    void deleteObjectsMayorThan(@Param("id_obj") Long id_obj);
}

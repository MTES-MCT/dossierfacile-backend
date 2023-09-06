package fr.dossierfacile.garbagecollector.repo.file;

import fr.dossierfacile.garbagecollector.model.file.GarbageFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileRepository extends JpaRepository<GarbageFile, Long> {

    @Query(value = """
            select path from storage_file sf where sf.path in (:path)
            """, nativeQuery = true)
    List<String> existingFiles(@Param("path") List<String> path);

}

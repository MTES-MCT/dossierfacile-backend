package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.StorageFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StorageFileRepository extends JpaRepository<StorageFile, Long> {
    List<StorageFile> findAllByName(String s);

    @Query(value = """
            SELECT *
            FROM storage_file sf
            WHERE array_length(sf.providers, 1) < 2
            AND last_modified_date is not null
            AND sf.last_modified_date < NOW() - INTERVAL '10' MINUTE 
            AND sf.last_modified_date > NOW() - INTERVAL '10' DAY
            ORDER BY sf.last_modified_date DESC
            """, nativeQuery = true)
    List<StorageFile> findAllWithOneProvider(Pageable pageable);

    List<StorageFile> findByPathIn(List<String> paths);

    @Query(value = "SELECT path FROM storage_file WHERE path IN (:pathsToSearch)",
            nativeQuery = true)
    List<String> findExistingPathsIn(@Param("pathsToSearch") List<String> paths);

}

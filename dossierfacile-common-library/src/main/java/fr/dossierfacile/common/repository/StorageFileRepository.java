package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StorageFileRepository extends JpaRepository<StorageFile, Long> {
    List<StorageFile> findAllByName(String s);

    @Query(value = """
            SELECT *
            FROM storage_file sf
            WHERE array_length(sf.providers, 1) < 2
            ORDER BY sf.last_modified_date DESC
            """, nativeQuery = true)
    Page<StorageFile> findAllWithOneProvider(Pageable pageable);

}
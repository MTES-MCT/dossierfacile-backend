package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStorageStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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
            AND sf.status is null
            ORDER BY sf.last_modified_date DESC
            """, nativeQuery = true)
    List<StorageFile> findAllWithOneProviderAndReady(Pageable pageable);

    @Query(value = """
            SELECT *
            FROM storage_file sf
            WHERE array_length(sf.providers, 1) < 2
            AND last_modified_date is not null
            AND sf.last_modified_date < NOW() - INTERVAL '10' MINUTE
            AND sf.last_modified_date > NOW() - INTERVAL '10' DAY
            AND sf.status = 'COPY_FAILED'
            ORDER BY sf.last_modified_date DESC
            """, nativeQuery = true)
    List<StorageFile> findAllWithOneProviderAndCopyFailed(Pageable pageable);


    @Query(value = "SELECT path FROM storage_file WHERE path IN (:pathsToSearch)",
            nativeQuery = true)
    List<String> findExistingPathsIn(@Param("pathsToSearch") List<String> paths);

    List<StorageFile> findAllByStatusOrderByIdAsc(FileStorageStatus fileStorageStatus, Pageable pageable);

    List<StorageFile> findAllByStatusAndProviderOrderByIdAsc(FileStorageStatus status, ObjectStorageProvider provider, Pageable pageable);

    List<StorageFile> findAllByStatusAndProviderAndLastModifiedDateBeforeOrderByIdAsc(
            FileStorageStatus status,
            ObjectStorageProvider provider,
            LocalDateTime lastModifiedDate,
            Pageable pageable
    );

    void delete(@NotNull StorageFile storageFile);

    // ============ Batch operations for bulk deletion ============

    /**
     * Delete multiple storage files by their IDs in a single query.
     * This is much more efficient than deleting one by one.
     *
     * @param ids Collection of storage file IDs to delete
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM StorageFile sf WHERE sf.id IN :ids")
    int deleteAllByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * Update status to DELETE_FAILED for multiple storage files.
     * Used when storage deletion fails for some files.
     *
     * @param ids Collection of storage file IDs to update
     * @param status The new status to set (typically DELETE_FAILED)
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE StorageFile sf SET sf.status = :status WHERE sf.id IN :ids")
    int updateStatusByIdIn(@Param("ids") Collection<Long> ids, @Param("status") FileStorageStatus status);

    /**
     * Count files pending deletion for a specific provider.
     * Useful for monitoring the backlog.
     *
     * @param status The status to count (typically TO_DELETE)
     * @param provider The storage provider
     * @return Count of matching files
     */
    @Query("SELECT COUNT(sf) FROM StorageFile sf WHERE sf.status = :status AND sf.provider = :provider")
    long countByStatusAndProvider(@Param("status") FileStorageStatus status, @Param("provider") ObjectStorageProvider provider);
}

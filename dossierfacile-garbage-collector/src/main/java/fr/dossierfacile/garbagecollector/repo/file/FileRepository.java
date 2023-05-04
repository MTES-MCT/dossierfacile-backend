package fr.dossierfacile.garbagecollector.repo.file;

import fr.dossierfacile.garbagecollector.model.file.GarbageFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileRepository extends JpaRepository<GarbageFile, Long> {

    @Query(value = """
            SELECT
            EXISTS (select d from Document d where d.name=:path) OR
            EXISTS (select f from File f where f.path=:path) OR
            EXISTS (select sf from storage_file sf where sf.path=:path)
            """, nativeQuery = true)
    boolean existsObject(@Param("path") String path);

    @Query(value = """
            select path from File f where f.path in (:path)\s
            UNION select path from storage_file sf where sf.path in (:path)\s
            UNION select name from Document d where d.name in (:path)
            """, nativeQuery = true)
    List<String> existingFiles(@Param("path") List<String> path);

    @Query(value = "SELECT distinct file.* " +
            "FROM file left join document on file.document_id=document.id " +
            "left join tenant on document.tenant_id=tenant.id " +
            "where tenant.status='ARCHIVED' " +
            "and file.path is not null and file.path <> '' " +
            " limit :limit", nativeQuery = true)
    List<GarbageFile> getArchivedFile(@Param("limit") Integer limit);

    @Query(value = "SELECT distinct file.* " +
            "FROM file left join document on file.document_id=document.id " +
            "left join guarantor on document.guarantor_id=guarantor.id " +
            "left join tenant on guarantor.tenant_id=tenant.id " +
            "where tenant.status='ARCHIVED' " +
            "and file.path is not null and file.path <> '' " +
            " limit :limit", nativeQuery = true)
    List<GarbageFile> getGuarantorArchivedFile(@Param("limit") Integer limit);

}

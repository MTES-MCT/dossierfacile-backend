package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {

    @Query(value = "select path FROM file WHERE document_id = :documentId", nativeQuery = true)
    List<String> getFilePathsByDocumentId(@Param("documentId") Long documentId);
}

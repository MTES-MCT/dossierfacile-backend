package fr.dossierfacile.api.pdfgenerator.repository;

import fr.dossierfacile.common.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    @Query("from File f left join fetch f.key where document_id = :documentId")
    List<File> findAllByDocumentId(@Param("documentId") Long documentId);
}

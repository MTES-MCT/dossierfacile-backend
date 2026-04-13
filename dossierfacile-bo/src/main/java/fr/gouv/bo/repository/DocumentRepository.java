package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Modifying
    @Query("UPDATE Document d SET d.documentDeniedReasons = :documentDeniedReasons where d.id = :documentId")
    void updateDocumentWithDocumentDeniedReasons(@Param("documentDeniedReasons") DocumentDeniedReasons documentDeniedReasons, @Param("documentId") Long documentId);

    Optional<Document> findByName(String name);
}

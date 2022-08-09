package fr.dossierfacile.process.file.repository;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.type.TaxDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Modifying
    @Query("UPDATE Document d SET d.taxProcessResult = :taxProcessResult where d.id = :documentId")
    void updateTaxProcessResult(@Param("taxProcessResult") TaxDocument taxProcessResult, @Param("documentId") Long documentId);

}

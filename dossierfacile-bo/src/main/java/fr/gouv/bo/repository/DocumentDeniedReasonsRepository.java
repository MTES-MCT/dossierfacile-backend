package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentDeniedReasonsRepository extends JpaRepository<DocumentDeniedReasons, Long> {

    @Modifying
    @Query("UPDATE DocumentDeniedReasons d SET d.message = :message where d.id in :documentDeniedReasonsId")
    void updateDocumentDeniedReasonsWithMessage(@Param("message") Message message, @Param("documentDeniedReasonsId") List<Long> documentDeniedReasonsId);
}

package fr.dossierfacile.api.pdfgenerator.repository;

import fr.dossierfacile.common.entity.Guarantor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GuarantorRepository extends JpaRepository<Guarantor, Long> {
    @Query(value = "select g FROM Guarantor g WHERE g.id in (select d.guarantor.id FROM Document d WHERE d.id = :documentId AND d.guarantor.id IS NOT NULL)")
    Optional<Guarantor> getGuarantorByDocumentId(@Param("documentId") Long documentId);
}

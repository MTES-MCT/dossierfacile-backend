package fr.dossierfacile.garbagecollector.repo.document;

import fr.dossierfacile.garbagecollector.model.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query(value = "SELECT distinct document.* " +
            "FROM document left join tenant on document.tenant_id=tenant.id " +
            "where tenant.status='ARCHIVED' and document.name is not null " +
            "and document.name <> '' " +
            "ORDER BY document.id desc LIMIT :limit", nativeQuery = true)
    List<Document> getArchivedDocumentWithPdf(@Param("limit") Integer limit);

    @Query(value = "SELECT distinct document.* " +
            "FROM document left join guarantor on document.guarantor_id=guarantor.id " +
            "LEFT JOIN tenant on guarantor.tenant_id=tenant.id " +
            "where tenant.status='ARCHIVED' and document.name is not null " +
            "and document.name <> '' " +
            "ORDER BY document.id desc LIMIT :limit", nativeQuery = true)
    List<Document> getGuarantorArchivedDocumentWithPdf(@Param("limit") Integer limit);
}

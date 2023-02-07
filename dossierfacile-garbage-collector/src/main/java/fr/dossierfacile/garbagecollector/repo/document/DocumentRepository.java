package fr.dossierfacile.garbagecollector.repo.document;

import fr.dossierfacile.garbagecollector.model.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query(value = "SELECT distinct document.* " +
            "FROM document left join tenant on document.tenant_id=tenant.id " +
            "left outer join tenant_userapi on tenant.id=tenant_userapi.tenant_id  " +
            "where tenant.status='ARCHIVED' and document.name is not null " +
            "and document.name <> '' and tenant_userapi.tenant_id is null " +
            "ORDER BY document.id desc LIMIT :limit", nativeQuery = true)
    List<Document> getArchivedDocumentWithPdf(@Param("limit") Integer limit);
}

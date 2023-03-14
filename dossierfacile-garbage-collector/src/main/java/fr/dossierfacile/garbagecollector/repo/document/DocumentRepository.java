package fr.dossierfacile.garbagecollector.repo.document;

import fr.dossierfacile.garbagecollector.model.document.GarbageDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<GarbageDocument, Long> {
    @Query(value = "SELECT distinct document.* " +
            "FROM document left join tenant on document.tenant_id=tenant.id " +
            "where tenant.status='ARCHIVED' and document.name is not null " +
            "and document.name <> '' " +
            "ORDER BY document.id desc LIMIT :limit", nativeQuery = true)
    List<GarbageDocument> getArchivedDocumentWithPdf(@Param("limit") Integer limit);

    @Query(value = "SELECT distinct document.* " +
            "FROM document left join guarantor on document.guarantor_id=guarantor.id " +
            "LEFT JOIN tenant on guarantor.tenant_id=tenant.id " +
            "where tenant.status='ARCHIVED' and document.name is not null " +
            "and document.name <> '' " +
            "ORDER BY document.id desc LIMIT :limit", nativeQuery = true)
    List<GarbageDocument> getGuarantorArchivedDocumentWithPdf(@Param("limit") Integer limit);

    @Query(value = "select d1.*\n" +
            "from document d1\n" +
            "where d1.tenant_id = :tenantId\n" +
            "union\n" +
            "select d2.*\n" +
            "from document d2\n" +
            "         join guarantor g on d2.guarantor_id = g.id\n" +
            "where g.tenant_id = :tenantId", nativeQuery = true)
    List<GarbageDocument> findAllAssociatedToTenantId(Long tenantId);
}

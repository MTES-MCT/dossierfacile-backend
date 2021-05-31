package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    long countByDocumentCategoryAndGuarantor(DocumentCategory documentCategory, Guarantor guarantor);

    Optional<Document> findByDocumentCategoryAndTenant(DocumentCategory identification, Tenant tenant);

    Optional<Document> findByDocumentCategoryAndTenantAndId(DocumentCategory financial, Tenant tenant, Long id);

    Optional<Document> findByDocumentCategoryAndGuarantor(DocumentCategory identification, Guarantor guarantor);

    Optional<Document> findByDocumentCategoryAndGuarantorAndId(DocumentCategory financial, Guarantor guarantor, Long documentId);

    @Query(value = "select d1.*\n" +
            "from document d1\n" +
            "where d1.tenant_id = :tenantId\n" +
            "  and d1.id = :documentId\n" +
            "union\n" +
            "select d2.*\n" +
            "from document d2\n" +
            "         join guarantor g on d2.guarantor_id = g.id\n" +
            "where g.tenant_id = :tenantId\n" +
            "  and d2.id = :documentId", nativeQuery = true)
    Optional<Document> findByIdAssociatedToTenantId(@Param("documentId") Long documentId, @Param("tenantId") Long tenantId);

    Optional<Document> findByName(String documentName);
}

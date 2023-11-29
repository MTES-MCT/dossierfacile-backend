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

    Optional<Document> findFirstByDocumentCategoryAndTenant(DocumentCategory documentCategory, Tenant tenant);

    Optional<Document> findByDocumentCategoryAndTenantAndId(DocumentCategory documentCategory, Tenant tenant, Long id);

    Optional<Document> findFirstByDocumentCategoryAndGuarantor(DocumentCategory documentCategory, Guarantor guarantor);

    Optional<Document> findByDocumentCategoryAndGuarantorAndId(DocumentCategory documentCategory, Guarantor guarantor, Long documentId);

    @Query(value = "select d1.*\n" +
            "from document d1\n" +
            "  join tenant t on t.id = d1.tenant_id\n" +
            "where t.apartment_sharing_id = :apartId\n" +
            "  and d1.id = :documentId\n" +
            "union\n" +
            "select d2.*\n" +
            "from document d2\n" +
            "  join guarantor g on d2.guarantor_id = g.id\n" +
            "  join tenant t on t.id = g.tenant_id\n" +
            "where t.apartment_sharing_id = :apartId\n" +
            "  and d2.id = :documentId", nativeQuery = true)
    Optional<Document> findByIdForApartmentSharing(@Param("documentId") Long documentId, @Param("apartId") Long apartmentSharing);

    Optional<Document> findFirstByName(String name);
}

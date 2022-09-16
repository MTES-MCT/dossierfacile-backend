package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.type.TaxDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
            "  join tenant t on t.id = d2.tenant_id\n" +
            "  join guarantor g on d2.guarantor_id = g.id\n" +
            "where t.apartment_sharing_id = :apartId\n" +
            "  and d2.id = :documentId", nativeQuery = true)
    Optional<Document> findByIdForApartmentSharing(@Param("documentId") Long documentId, @Param("apartId") Long apartmentSharing);

    Optional<Document> findFirstByName(String documentName);

    @Query(value = "select d1.*\n" +
            "from document d1\n" +
            "where d1.tenant_id = :tenantId\n" +
            "union\n" +
            "select d2.*\n" +
            "from document d2\n" +
            "         join guarantor g on d2.guarantor_id = g.id\n" +
            "where g.tenant_id = :tenantId", nativeQuery = true)
    List<Document> findAllAssociatedToTenantId(Long tenantId);

    @Modifying
    @Query("UPDATE Document d SET d.taxProcessResult = :taxProcessResult where d.id = :documentId")
    void updateTaxProcessResult(@Param("taxProcessResult") TaxDocument taxProcessResult, @Param("documentId") Long documentId);
}

package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    @Query("select count(f) from File f join f.document d join d.tenant t where d.documentCategory =:documentCategory and t=:tenant")
    long countFileByDocumentCategoryTenant(@Param("documentCategory") DocumentCategory documentCategory, @Param("tenant") Tenant tenant);

    @Query("select sum(f.numberOfPages) from File f join f.document d join d.tenant t where d.documentCategory =:documentCategory and t =:tenant")
    Integer countNumberOfPagesByDocumentCategoryAndTenant(@Param("documentCategory") DocumentCategory documentCategory, @Param("tenant") Tenant tenant);

    @Query("select sum(f.numberOfPages) from File f join f.document d join d.tenant t where d.id =:documentId and t =:tenant")
    Integer countNumberOfPagesByDocumentIdAndTenant(@Param("documentId") long documentId, @Param("tenant") Tenant tenant);

    @Query("select sum(f.numberOfPages) from File f join f.document d join d.guarantor g join g.tenant t where d.documentCategory =:documentCategory and g.id =:guarantorId and t =:tenant")
    Integer countNumberOfPagesByDocumentCategoryAndGuarantorIdAndTenant(@Param("documentCategory") DocumentCategory documentCategory, @Param("guarantorId") long guarantorId, @Param("tenant") Tenant tenant);

    @Query("select sum(f.numberOfPages) from File f join f.document d join d.guarantor g join g.tenant t where d.id =:documentId and g.id =:guarantorId and t=:tenant")
    Integer countNumberOfPagesByDocumentIdAndGuarantorIdAndTenant(@Param("documentId") long documentId, @Param("guarantorId") long guarantorId, @Param("tenant") Tenant tenant);

    @Query("select sum(f.size) from File f join f.document d join d.tenant t where d.documentCategory =:documentCategory and t=:tenant")
    long sumSizeOfAllFilesForDocument(@Param("documentCategory") DocumentCategory documentCategory, @Param("tenant") Tenant tenant);

    @Query("select count(f) from File f join f.document d join d.tenant t where d.documentCategory =:documentCategory and t=:tenant and d.id=:documentId")
    long countFileByDocumentCategoryTenantDocumentId(@Param("documentCategory") DocumentCategory documentCategory, @Param("tenant") Tenant tenant, @Param("documentId") Long documentId);

    @Query("select sum(f.size) from File f join f.document d join d.tenant t where d.documentCategory =:documentCategory and t=:tenant and d.id=:documentId")
    long sumSizeOfAllFilesForDocumentId(@Param("documentCategory") DocumentCategory documentCategory, @Param("tenant") Tenant tenant, @Param("documentId") Long documentId);

    @Query("select count(f) from File f " +
            "join f.document d join d.guarantor g join g.tenant t " +
            "where d.documentCategory =:documentCategory and g.id =:guarantorId and g.typeGuarantor =:typeGuarantor and t=:tenant")
    long countFileByDocumentCategoryGuarantorIdTypeGuarantorTenant(
            @Param("documentCategory") DocumentCategory documentCategory,
            @Param("guarantorId") Long guarantorId,
            @Param("typeGuarantor") TypeGuarantor typeGuarantor,
            @Param("tenant") Tenant tenant);

    @Query("select sum(f.size) from File f " +
            "join f.document d join d.guarantor g join g.tenant t " +
            "where d.documentCategory =:documentCategory and g.id =:guarantorId and g.typeGuarantor =:typeGuarantor and t=:tenant")
    long sumSizeOfAllFilesInDocumentForGuarantorTenantId(
            @Param("documentCategory") DocumentCategory documentCategory,
            @Param("guarantorId") Long guarantorId,
            @Param("typeGuarantor") TypeGuarantor typeGuarantor,
            @Param("tenant") Tenant tenant);


    @Query("select count(f) from File f " +
            "join f.document d join d.guarantor g join g.tenant t " +
            "where d.documentCategory =:documentCategory and g.id =:guarantorId and g.typeGuarantor =:typeGuarantor and t=:tenant and d.id=:documentId")
    long countFileByDocumentCategoryGuarantorIdTypeGuarantorTenantDocumentId(
            @Param("documentCategory") DocumentCategory documentCategory,
            @Param("guarantorId") Long guarantorId,
            @Param("typeGuarantor") TypeGuarantor typeGuarantor,
            @Param("tenant") Tenant tenant,
            @Param("documentId") Long documentId);


    @Query("select sum(f.size) from File f " +
            "join f.document d join d.guarantor g join g.tenant t " +
            "where d.documentCategory =:documentCategory and g.id =:guarantorId and g.typeGuarantor =:typeGuarantor and t=:tenant and d.id=:documentId")
    long sumSizeOfAllFilesInDocumentForGuarantorTenantId(
            @Param("documentCategory") DocumentCategory documentCategory,
            @Param("guarantorId") Long guarantorId,
            @Param("typeGuarantor") TypeGuarantor typeGuarantor,
            @Param("tenant") Tenant tenant,
            @Param("documentId") Long documentId);


    @Query("select sum(f.size) from File f " +
            "join f.document d join d.guarantor g join g.tenant t " +
            "where d.documentCategory =:documentCategory and g.id =:guarantorId and g.typeGuarantor =:typeGuarantor and t=:tenant")
    long sumSizeOfAllFilesInDocumentForGuarantorTenant(
            @Param("documentCategory") DocumentCategory documentCategory,
            @Param("guarantorId") Long guarantorId,
            @Param("typeGuarantor") TypeGuarantor typeGuarantor,
            @Param("tenant") Tenant tenant);

    @Query(value = "select f.*\n" +
            "from file f\n" +
            "         join document d on f.document_id = d.id\n" +
            "where d.tenant_id = :t\n" +
            "  and f.id = :id\n" +
            "union\n" +
            "select f2.*\n" +
            "from file f2\n" +
            "         join document d2 on f2.document_id = d2.id\n" +
            "         join guarantor g on d2.guarantor_id = g.id\n" +
            "where g.tenant_id = :t\n" +
            "  and f2.id = :id", nativeQuery = true)
    Optional<File> findByIdAndTenant(@Param("id") Long id, @Param("t") Long t);

    @Query(value = "select path FROM file WHERE document_id = :documentId", nativeQuery = true)
    List<String> getFilePathsByDocumentId(@Param("documentId") Long documentId);
}

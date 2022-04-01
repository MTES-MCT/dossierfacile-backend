package fr.dossierfacile.api.pdfgenerator.repository;

import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    @Query(value = "select t FROM Tenant t WHERE t.id in (select d.tenant.id FROM Document d WHERE d.id = :documentId AND d.tenant.id IS NOT NULL)")
    Optional<Tenant> getTenantByDocumentId(@Param("documentId") Long documentId);
}

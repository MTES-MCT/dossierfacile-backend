package fr.dossierfacile.process.file.repository;

import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByIdAndFirstNameIsNotNullAndLastNameIsNotNull(Long tenantId);
}

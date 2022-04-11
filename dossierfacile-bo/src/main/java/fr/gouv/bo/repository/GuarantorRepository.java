package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuarantorRepository extends JpaRepository<Guarantor, Long> {
    Optional<Guarantor> findByIdAndTenant(Long id, Tenant tenant);
}

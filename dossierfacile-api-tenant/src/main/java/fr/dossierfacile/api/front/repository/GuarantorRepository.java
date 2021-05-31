package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuarantorRepository extends JpaRepository<Guarantor, Long> {

    Optional<Guarantor> findByTenantAndTypeGuarantor(Tenant tenant, TypeGuarantor typeGuarantor);

    Optional<Guarantor> findByTenantAndTypeGuarantorAndId(Tenant tenant, TypeGuarantor typeGuarantor, Long id);

    boolean existsByIdAndTenantAndTypeGuarantor(Long guarantorId, Tenant tenant, TypeGuarantor typeGuarantor);

    Optional<Guarantor> findByIdAndTenant(Long id, Tenant tenant);
}

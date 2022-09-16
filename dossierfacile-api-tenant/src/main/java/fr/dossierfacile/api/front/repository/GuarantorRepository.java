package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuarantorRepository extends JpaRepository<Guarantor, Long> {

    Optional<Guarantor> findFirstByTenantAndTypeGuarantor(Tenant tenant, TypeGuarantor typeGuarantor);

    Optional<Guarantor> findByTenantAndTypeGuarantorAndId(Tenant tenant, TypeGuarantor typeGuarantor, Long id);

    boolean existsByIdAndTenantAndTypeGuarantor(Long guarantorId, Tenant tenant, TypeGuarantor typeGuarantor);

    Optional<Guarantor> findByIdAndTenant(Long id, Tenant tenant);

    List<Guarantor> findGuarantorsByTenant(Tenant tenant);

    @Query(value = "select g.*\n" +
            "from guarantor g\n" +
            "  join tenant t on t.id = g.tenant_id\n" +
            "where t.apartment_sharing_id = :apartId\n" +
            "  and g.id = :guarantorId\n", nativeQuery = true)
    Optional<Guarantor> findByIdForApartmentSharing(@Param("guarantorId") Long id, @Param("apartId") Long apartmentSharingId);
}

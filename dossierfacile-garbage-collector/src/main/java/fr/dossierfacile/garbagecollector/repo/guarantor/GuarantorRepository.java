package fr.dossierfacile.garbagecollector.repo.guarantor;

import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuarantorRepository extends JpaRepository<Guarantor, Long> {

    List<Guarantor> findGuarantorsByTenant(Tenant tenant);
}

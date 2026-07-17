package fr.dossierfacile.common.infrastructure.repository;

import fr.dossierfacile.common.domain.model.guarantor.Guarantor;
import fr.dossierfacile.common.infrastructure.entity.GuarantorEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository d'infrastructure pour l'agrégat Guarantor.
 * Encapsule JpaGuarantorEntityRepository et convertit les entités en modèles de domaine.
 */
@Repository
public class JpaGuarantorRepository implements JpaRepository {

    private final JpaGuarantorEntityRepository jpaGuarantorEntityRepository;

    JpaGuarantorRepository(JpaGuarantorEntityRepository jpaGuarantorEntityRepository) {
        this.jpaGuarantorEntityRepository = jpaGuarantorEntityRepository;
    }

    public Optional<Guarantor> findById(Long id) {
        return jpaGuarantorEntityRepository.findById(id)
                .map(Guarantor::new);
    }

    public List<Guarantor> findByTenantId(Long tenantId) {
        return jpaGuarantorEntityRepository.findByTenantId(tenantId).stream()
                .map(Guarantor::new)
                .toList();
    }

    public List<Guarantor> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaGuarantorEntityRepository.findAllById(ids).stream()
                .map(Guarantor::new)
                .toList();
    }

    public void save(Guarantor guarantor) {
        jpaGuarantorEntityRepository.save(guarantor.getEntityOnlyForRepository());
    }

    public void delete(Guarantor guarantor) {
        jpaGuarantorEntityRepository.delete(guarantor.getEntityOnlyForRepository());
    }
}

/**
 * Interface Spring Data JPA interne (package-private).
 */
interface JpaGuarantorEntityRepository extends org.springframework.data.jpa.repository.JpaRepository<GuarantorEntity, Long> {

    List<GuarantorEntity> findByTenantId(Long tenantId);
}

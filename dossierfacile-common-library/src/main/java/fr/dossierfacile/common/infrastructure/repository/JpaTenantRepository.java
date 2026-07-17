package fr.dossierfacile.common.infrastructure.repository;

import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository d'infrastructure pour l'agrégat Tenant.
 * Cette classe encapsule le repository Spring Data JPA pour TenantEntity
 * et convertit/enveloppe les entités de persistance dans le type du domaine public {@link Tenant}.
 */
@Repository
public class JpaTenantRepository implements JpaRepository {

    private final JpaTenantEntityRepository jpaTenantEntityRepository;

    // Le constructeur est package-private pour ne pas exposer l'interface JpaTenantEntityRepository
    // en dehors de ce package (évite le warning de visibilité). Spring l'injectera sans problème.
    JpaTenantRepository(JpaTenantEntityRepository jpaTenantEntityRepository) {
        this.jpaTenantEntityRepository = jpaTenantEntityRepository;
    }

    /**
     * Recherche un locataire par son ID.
     */
    public Optional<Tenant> findById(Long id) {
        return jpaTenantEntityRepository.findById(id)
                .map(Tenant::new);
    }

    /**
     * Recherche un locataire par son identifiant Keycloak.
     */
    public Optional<Tenant> findByKeycloakId(String keycloakId) {
        return jpaTenantEntityRepository.findByKeycloakId(keycloakId)
                .map(Tenant::new);
    }

    /**
     * Recherche un locataire par son e-mail.
     */
    public Optional<Tenant> findByEmail(String email) {
        return jpaTenantEntityRepository.findByEmail(email)
                .map(Tenant::new);
    }

    /**
     * Recherche un locataire par l'ID de l'un de ses garants.
     */
    public Optional<Tenant> findByGuarantorId(Long guarantorId) {
        return jpaTenantEntityRepository.findByGuarantorId(guarantorId)
                .map(Tenant::new);
    }

    /**
     * Sauvegarde l'état de l'agrégat en persistant l'entité interne.
     */
    public void save(Tenant tenant) {
        jpaTenantEntityRepository.save(tenant.getEntityOnlyForRepository());
    }

    /**
     * Sauvegarde et valide immédiatement en base de données.
     */
    public void saveAndFlush(Tenant tenant) {
        jpaTenantEntityRepository.saveAndFlush(tenant.getEntityOnlyForRepository());
    }
}

/**
 * Interface Spring Data JPA interne (package-private) pour la persistence de TenantEntity.
 * Non visible en dehors de ce package pour forcer l'usage exclusif de JpaTenantRepository.
 */
interface JpaTenantEntityRepository extends org.springframework.data.jpa.repository.JpaRepository<TenantEntity, Long> {
    Optional<TenantEntity> findByKeycloakId(String keycloakId);
    Optional<TenantEntity> findByEmail(String email);

    @Query("select t from TenantEntity t join t.guarantorIds g where g = :guarantorId")
    Optional<TenantEntity> findByGuarantorId(@Param("guarantorId") Long guarantorId);
}

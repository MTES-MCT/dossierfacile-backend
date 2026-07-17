package fr.dossierfacile.common.infrastructure.repository;

import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.infrastructure.entity.ApartmentSharingEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository d'infrastructure pour l'agrégat ApartmentSharing.
 * Cette classe encapsule le repository Spring Data JPA pour ApartmentSharingEntity
 * et convertit/enveloppe les entités de persistance dans le type du domaine public {@link ApartmentSharing}.
 */
@Repository
public class JpaApartmentSharingRepository implements JpaRepository {

    private final JpaApartmentSharingEntityRepository jpaApartmentSharingEntityRepository;

    JpaApartmentSharingRepository(JpaApartmentSharingEntityRepository jpaApartmentSharingEntityRepository) {
        this.jpaApartmentSharingEntityRepository = jpaApartmentSharingEntityRepository;
    }

    /**
     * Recherche un dossier de candidature par son ID.
     */
    public Optional<ApartmentSharing> findById(Long id) {
        return jpaApartmentSharingEntityRepository.findById(id)
                .map(ApartmentSharing::new);
    }

    /**
     * Sauvegarde l'état de l'agrégat en persistant l'entité interne.
     */
    public void save(ApartmentSharing apartmentSharing) {
        jpaApartmentSharingEntityRepository.save(apartmentSharing.getEntityOnlyForRepository());
    }

    /**
     * Sauvegarde et valide immédiatement en base de données.
     */
    public void saveAndFlush(ApartmentSharing apartmentSharing) {
        jpaApartmentSharingEntityRepository.saveAndFlush(apartmentSharing.getEntityOnlyForRepository());
    }
}

/**
 * Interface Spring Data JPA interne (package-private) pour la persistence de ApartmentSharingEntity.
 * Non visible en dehors de ce package pour forcer l'usage exclusif de JpaApartmentSharingRepository.
 */
interface JpaApartmentSharingEntityRepository extends org.springframework.data.jpa.repository.JpaRepository<ApartmentSharingEntity, Long> {
}

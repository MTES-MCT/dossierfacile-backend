package fr.dossierfacile.common.infrastructure.repository;

import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.infrastructure.entity.OperatorEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository d'infrastructure pour l'agrégat Operator.
 * Cette classe encapsule le repository Spring Data JPA pour OperatorEntity
 * et convertit/enveloppe les entités de persistance dans le type du domaine public {@link Operator}.
 */
@Repository
public class JpaOperatorRepository implements JpaRepository {

    private final JpaOperatorEntityRepository jpaOperatorEntityRepository;

    JpaOperatorRepository(JpaOperatorEntityRepository jpaOperatorEntityRepository) {
        this.jpaOperatorEntityRepository = jpaOperatorEntityRepository;
    }

    public Optional<Operator> findById(Long id) {
        return jpaOperatorEntityRepository.findById(id)
                .map(Operator::new);
    }

    public Optional<Operator> findByEmail(String email) {
        return jpaOperatorEntityRepository.findByEmail(email)
                .map(Operator::new);
    }

    public Optional<Operator> findByKeycloakId(String keycloakId) {
        return jpaOperatorEntityRepository.findByKeycloakId(keycloakId)
                .map(Operator::new);
    }

    public Operator save(Operator operator) {
        OperatorEntity savedEntity = jpaOperatorEntityRepository.save(operator.getEntityOnlyForRepository());
        return new Operator(savedEntity);
    }
}

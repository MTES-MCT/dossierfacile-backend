package fr.dossierfacile.common.infrastructure.repository;

import fr.dossierfacile.common.infrastructure.entity.OperatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface JpaOperatorEntityRepository extends JpaRepository<OperatorEntity, Long> {
    Optional<OperatorEntity> findByEmail(String email);
    Optional<OperatorEntity> findByKeycloakId(String keycloakId);
}

package fr.dossierfacile.common.infrastructure.repository;

import fr.dossierfacile.common.infrastructure.entity.OperatorEntity;

import java.util.Optional;

interface JpaOperatorEntityRepository extends org.springframework.data.jpa.repository.JpaRepository<OperatorEntity, Long> {
    Optional<OperatorEntity> findByEmail(String email);
    Optional<OperatorEntity> findByKeycloakId(String keycloakId);
}

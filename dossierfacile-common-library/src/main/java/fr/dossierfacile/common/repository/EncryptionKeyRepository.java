package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.EncryptionKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EncryptionKeyRepository extends JpaRepository<EncryptionKey, Long> {
    Optional<EncryptionKey> findByStatus(EncryptionKeyStatus status);
}

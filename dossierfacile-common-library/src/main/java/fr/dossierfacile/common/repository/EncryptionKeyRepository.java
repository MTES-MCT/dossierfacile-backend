package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.EncryptionKeyStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EncryptionKeyRepository extends JpaRepository<EncryptionKey, Long> {
    @Cacheable("encryption-key")
    Optional<EncryptionKey> findByStatus(EncryptionKeyStatus status);
}

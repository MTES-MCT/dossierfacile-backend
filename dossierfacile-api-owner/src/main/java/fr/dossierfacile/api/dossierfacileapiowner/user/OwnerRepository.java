package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.common.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long> {
    Optional<Owner> findByEmailAndEnabledFalse(String email);

    Optional<Owner> findByEmail(String email);

    Optional<Owner> findByKeycloakId(String keycloakId);
}

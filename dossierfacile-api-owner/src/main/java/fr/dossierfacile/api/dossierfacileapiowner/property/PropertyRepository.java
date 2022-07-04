package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findAllByOwnerId(Long ownerId);

    Optional<Property> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<Property> findByToken(String token);
}

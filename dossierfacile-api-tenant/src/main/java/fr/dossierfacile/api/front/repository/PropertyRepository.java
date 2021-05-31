package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    Optional<Property> findFirstByToken(String token);
}

package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {
}
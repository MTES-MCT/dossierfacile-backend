package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.PropertyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyIdRepository extends JpaRepository<PropertyId, Integer> {
}

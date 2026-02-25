package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {
}


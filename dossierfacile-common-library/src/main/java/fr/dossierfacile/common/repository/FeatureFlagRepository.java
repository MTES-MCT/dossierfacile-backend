package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {
    List<FeatureFlag> findAllByOrderByCreatedAtDesc();
}


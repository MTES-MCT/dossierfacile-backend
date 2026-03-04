package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.FeatureFlag;

import java.util.List;

public interface FeatureFlagService {
    boolean isFeatureEnabledForUser(Long userId, String key);
    boolean isFeatureEnabledForUser(Long userId, FeatureFlag featureFlag);

    List<FeatureFlag> getAllFeatureFlags();

    void updateRolloutForFeatureFlag(FeatureFlag featureFlag, int newValue);

    void toggleFeatureFlag(FeatureFlag featureFlag, boolean b);

    FeatureFlag getFeatureFlag(String key);
}

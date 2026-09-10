package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.FeatureFlag;
import fr.dossierfacile.common.entity.UserApi;

import java.util.Collection;
import java.util.List;

public interface FeatureFlagService {
    boolean isFeatureEnabledForUser(Long userId, String key);
    boolean isFeatureEnabledForUser(Long userId, FeatureFlag featureFlag);
    boolean isFeatureEnabled(String key);

    /**
     * Partner-scoped flag: active and the partner's name is listed in its opted-in partners.
     * The owner partner ({@code dfconnect-proprietaire}) is never opted in.
     */
    boolean isPartnerOptedIn(String key, UserApi userApi);

    /**
     * Replaces the opted-in partner list (names are trimmed and deduplicated).
     *
     * @throws IllegalArgumentException when the owner partner is listed
     */
    void updateOptedInPartners(FeatureFlag featureFlag, Collection<String> partnerNames);

    List<FeatureFlag> getAllFeatureFlags();

    void updateRolloutForFeatureFlag(FeatureFlag featureFlag, int newValue);

    void toggleFeatureFlag(FeatureFlag featureFlag, boolean b);

    FeatureFlag getFeatureFlag(String key);
}

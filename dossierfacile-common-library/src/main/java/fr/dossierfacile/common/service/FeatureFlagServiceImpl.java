package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.FeatureFlag;
import fr.dossierfacile.common.entity.UserFeatureAssignment;
import fr.dossierfacile.common.entity.UserFeatureAssignmentHistory;
import fr.dossierfacile.common.entity.UserFeatureAssignmentId;
import fr.dossierfacile.common.enums.FeatureAssignmentReason;
import fr.dossierfacile.common.enums.FeatureAssignmentSource;
import fr.dossierfacile.common.repository.FeatureFlagRepository;
import fr.dossierfacile.common.repository.UserAccountRepository;
import fr.dossierfacile.common.repository.UserFeatureAssignmentHistoryRepository;
import fr.dossierfacile.common.repository.UserFeatureAssignmentRepository;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.UserFeatureAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeatureFlagServiceImpl implements FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final UserFeatureAssignmentRepository featureAssignmentRepository;
    private final UserFeatureAssignmentHistoryRepository featureAssignmentHistoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserFeatureAssignmentService userFeatureAssignmentService;

    @Transactional
    public boolean isFeatureEnabledForUser(Long userId, String key) {
        var featureFlag = featureFlagRepository.findById(key);
        if (featureFlag.isPresent()) {
            return checkAndAssign(userId, featureFlag.get());
        } else {
            log.warn("Feature flag with key {} not found", key);
            return false;
        }
    }

    @Transactional
    public boolean isFeatureEnabledForUser(Long userId, FeatureFlag featureFlag) {
        return checkAndAssign(userId, featureFlag);
    }

    private boolean checkAndAssign(Long userId, FeatureFlag featureFlag) {
        if (featureFlag == null || featureFlag.getKey() == null) {
            return false;
        }

        if (!featureFlag.isActive()){
            return false;
        }

        // If the feature is already calculated for this user, return the assigned value
        var userAssignment = featureAssignmentRepository.findById(new UserFeatureAssignmentId(userId, featureFlag.getKey()));
        if (userAssignment.isPresent()) {
            return userAssignment.get().isEnabled();
        }

        var user = userAccountRepository.findById(userId);
        if (user.isEmpty()) {
            log.warn("User with id {} not found when checking feature flag {}", userId, featureFlag.getKey());
            return false;
        }

        var userAssignmentBuilder = UserFeatureAssignment.builder()
                .id(new UserFeatureAssignmentId(userId, featureFlag.getKey()))
                .user(user.get())
                .featureFlag(featureFlag)
                .rolloutPct(featureFlag.getRolloutPct());

        var userAssignmentHistoryBuilder = UserFeatureAssignmentHistory.builder()
                .user(user.get())
                .featureFlag(featureFlag)
                .rolloutPct(featureFlag.getRolloutPct())
                .reason(FeatureAssignmentReason.FIRST_CHECK);

        // Check if the user has been created after the feature flag creation date or not
        if (featureFlag.isOnlyForNewUser() && user.get().getCreationDateTime().isBefore(featureFlag.getDeploymentDate())) {
            userAssignmentBuilder.enabled(false)
                    .bucket(0)
                    .assignmentSource(FeatureAssignmentSource.PRE_DEPLOYMENT);

            userAssignmentHistoryBuilder.enabled(false)
                    .bucket(0);

            return userFeatureAssignmentService.saveAssignment(userAssignmentBuilder.build(), userAssignmentHistoryBuilder.build(), false);
        }

        // Otherwise, compute the bucket for this user and assign the feature flag based on the rollout percentage
        int bucket = computeBucket(userId, featureFlag.getKey());
        boolean enabled = bucket < featureFlag.getRolloutPct();
        userAssignmentBuilder.enabled(enabled)
                .bucket(bucket)
                .assignmentSource(FeatureAssignmentSource.HASH);
        userAssignmentHistoryBuilder.enabled(enabled)
                .bucket(bucket);

        return userFeatureAssignmentService.saveAssignment(userAssignmentBuilder.build(), userAssignmentHistoryBuilder.build(), enabled);
    }

    @Transactional
    public void updateRolloutForFeatureFlag(FeatureFlag featureFlag, int newRolloutValue) {
        var lastValue = featureFlag.getRolloutPct();
        if (lastValue == newRolloutValue) {
            return;
        }
        featureFlag.setRolloutPct(newRolloutValue);
        featureFlagRepository.save(featureFlag);

        FeatureAssignmentReason reason = lastValue < newRolloutValue ? FeatureAssignmentReason.ROLLOUT_INCREASED : FeatureAssignmentReason.ROLLBACK;

        featureAssignmentHistoryRepository.saveHistoryForChangingAssignments(featureFlag.getKey(), newRolloutValue, reason.name());
        featureAssignmentRepository.updateRolloutPctAndEnabled(featureFlag.getKey(), newRolloutValue, FeatureAssignmentSource.HASH);
    }

    @Transactional
    public void toggleFeatureFlag(FeatureFlag featureFlag, boolean enabled) {
        if (featureFlag.isActive() == enabled) {
            return;
        }
        featureFlag.setActive(enabled);
        featureFlagRepository.save(featureFlag);
    }

    @Transactional(readOnly = true)
    public FeatureFlag getFeatureFlag(String key) {
        return featureFlagRepository.findById(key).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<FeatureFlag> getAllFeatureFlags() {
        return featureFlagRepository.findAllByOrderByCreatedAtDesc();
    }

    // Private package
    int computeBucket(Long userId, String featureKey) {
        try {
            String input = userId + "/" + featureKey;
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return getBucketFromHash(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable on this JVM", e);
        }
    }

    // Private package
    int getBucketFromHash(byte[] hash) {
        if (hash.length < 4) {
            return 0;
        }
        int value = Math.abs(
                ((hash[0] & 0x7F) << 24) | // Use 0x7F to ignore sign bit and ensure positive integer
                        ((hash[1] & 0xFF) << 16) |
                        ((hash[2] & 0xFF) << 8) |
                        (hash[3] & 0xFF)
        );
        return value % 100;
    }
}

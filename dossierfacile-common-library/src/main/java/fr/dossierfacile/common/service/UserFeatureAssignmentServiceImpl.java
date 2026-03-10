package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.UserFeatureAssignment;
import fr.dossierfacile.common.entity.UserFeatureAssignmentHistory;
import fr.dossierfacile.common.repository.FeatureFlagRepository;
import fr.dossierfacile.common.repository.UserAccountRepository;
import fr.dossierfacile.common.repository.UserFeatureAssignmentHistoryRepository;
import fr.dossierfacile.common.repository.UserFeatureAssignmentRepository;
import fr.dossierfacile.common.service.interfaces.UserFeatureAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserFeatureAssignmentServiceImpl implements UserFeatureAssignmentService {

    private final UserFeatureAssignmentRepository featureAssignmentRepository;
    private final UserFeatureAssignmentHistoryRepository featureAssignmentHistoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final FeatureFlagRepository featureFlagRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveAssignment(UserFeatureAssignment assignment, UserFeatureAssignmentHistory history, boolean fallbackEnabled) {
        Long userId = assignment.getId() != null ? assignment.getId().getUserId() : null;
        String featureKey = assignment.getId() != null ? assignment.getId().getFeatureKey() : null;

        if (userId == null || featureKey == null) {
            log.error("saveAssignment called with invalid assignment id: userId={}, featureKey={}", userId, featureKey);
            return fallbackEnabled;
        }

        try {
            // Use managed references in this transaction to guarantee FK binding on both tables.
            // It was modified after some issue when the user bind came from a side loaded entity and not a direct loaded user.
            // The fact we are inside a new transaction the lazy loaded entities loaded outside this new Transaction are not in the context resulting to a FK error on userID on save method.
            var managedUser = userAccountRepository.getReferenceById(userId);
            var managedFeature = featureFlagRepository.getReferenceById(featureKey);

            assignment.setUser(managedUser);
            assignment.setFeatureFlag(managedFeature);
            history.setUser(managedUser);
            history.setFeatureFlag(managedFeature);

            featureAssignmentRepository.saveAndFlush(assignment);
            featureAssignmentHistoryRepository.saveAndFlush(history);

            log.info("Feature flag {} enabled for user {}", featureKey, userId);
            return fallbackEnabled;
        } catch (DataIntegrityViolationException e) {
            log.error("saveAssignment failed for userId={}, feature={}", userId, featureKey, e);
            return featureAssignmentRepository.findById(assignment.getId())
                    .map(UserFeatureAssignment::isEnabled)
                    .orElse(fallbackEnabled);
        }
    }
}

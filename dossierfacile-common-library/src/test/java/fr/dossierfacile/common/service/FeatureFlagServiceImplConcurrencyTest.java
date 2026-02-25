package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.FeatureFlag;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserFeatureAssignmentId;
import fr.dossierfacile.common.repository.FeatureFlagRepository;
import fr.dossierfacile.common.repository.UserAccountRepository;
import fr.dossierfacile.common.repository.UserFeatureAssignmentHistoryRepository;
import fr.dossierfacile.common.repository.UserFeatureAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceImplConcurrencyTest {

    @Mock
    private FeatureFlagRepository featureFlagRepository;
    @Mock
    private UserFeatureAssignmentRepository featureAssignmentRepository;
    @Mock
    private UserFeatureAssignmentHistoryRepository featureAssignmentHistoryRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserFeatureAssignmentServiceImpl userFeatureAssignmentService; // MOCK DE LA NOUVELLE DEPENDANCE

    @InjectMocks
    private FeatureFlagServiceImpl featureFlagService;

    @Test
    void should_handle_race_condition_gracefully() throws Exception {
        // GIVEN
        Long userId = 1L;
        String featureKey = "race-condition-feature";
        FeatureFlag featureFlag = FeatureFlag.builder()
                .key(featureKey)
                .active(true)
                .rolloutPct(50)
                .onlyForNewUser(false)
                .build();

        User user = mock(User.class);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));

        // Initial check returns empty
        when(featureAssignmentRepository.findById(any(UserFeatureAssignmentId.class)))
                .thenReturn(Optional.empty());

        when(userFeatureAssignmentService.saveAssignment(any(), any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(2));

        // WHEN
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Boolean> featureEnabled1 = executor.submit(() -> featureFlagService.isFeatureEnabledForUser(userId, featureFlag));
        Future<Boolean> featureEnabled2 = executor.submit(() -> featureFlagService.isFeatureEnabledForUser(userId, featureFlag));

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // THEN
        // Both should succeed and return result (no exception)
        assertThat(featureEnabled1.get()).isNotNull();
        assertThat(featureEnabled2.get()).isNotNull();
    }
}

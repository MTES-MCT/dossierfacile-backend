package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.FeatureAssignmentReason;
import fr.dossierfacile.common.enums.FeatureAssignmentSource;
import fr.dossierfacile.common.repository.FeatureFlagRepository;
import fr.dossierfacile.common.repository.UserAccountRepository;
import fr.dossierfacile.common.repository.UserFeatureAssignmentHistoryRepository;
import fr.dossierfacile.common.repository.UserFeatureAssignmentRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceImplTest {

    @Mock
    private FeatureFlagRepository featureFlagRepository;
    @Mock
    private UserFeatureAssignmentRepository featureAssignmentRepository;
    @Mock
    private UserFeatureAssignmentHistoryRepository featureAssignmentHistoryRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserFeatureAssignmentServiceImpl userFeatureAssignmentService;

    @InjectMocks
    private FeatureFlagServiceImpl featureFlagService;

    @Nested
    class IsFeatureEnabledForUser {

        @Test
        void should_return_false_when_feature_flag_is_null_or_key_is_null() {
            assertThat(featureFlagService.isFeatureEnabledForUser(1L, "blabla")).isFalse();
            assertThat(featureFlagService.isFeatureEnabledForUser(1L, new FeatureFlag())).isFalse();
        }

        @Test
        void should_return_existing_value_without_fetching_user_if_assignment_exists() {
            // GIVEN
            Long userId = 123L;
            String featureKey = "new-dashboard";
            FeatureFlag featureFlag = FeatureFlag.builder().key(featureKey).active(true).build();

            UserFeatureAssignment existingAssignment = UserFeatureAssignment.builder()
                    .id(new UserFeatureAssignmentId(userId, featureKey))
                    .enabled(true)
                    .build();

            when(featureAssignmentRepository.findById(any(UserFeatureAssignmentId.class)))
                    .thenReturn(Optional.of(existingAssignment));

            // WHEN
            boolean result = featureFlagService.isFeatureEnabledForUser(userId, featureFlag);

            // THEN
            assertThat(result).isTrue();

            // IMPORTANT: Verify that userAccountRepository was NOT called
            verify(userAccountRepository, never()).findById(any());
            verify(featureAssignmentRepository, never()).save(any());
            verify(featureAssignmentHistoryRepository, never()).save(any());
        }

        @Test
        void should_return_false_if_user_does_not_exist() {
            // GIVEN
            Long userId = 999L;
            FeatureFlag featureFlag = FeatureFlag.builder().key("test-feature").active(true).build();

            when(featureAssignmentRepository.findById(any())).thenReturn(Optional.empty());
            when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

            // WHEN
            boolean result = featureFlagService.isFeatureEnabledForUser(userId, featureFlag);

            // THEN
            assertThat(result).isFalse();
            verify(featureAssignmentRepository, never()).save(any());
            verify(featureAssignmentHistoryRepository, never()).save(any());
        }

        @Test
        void should_return_false_if_feature_disabled() {
            // GIVEN
            Long userId = 999L;
            FeatureFlag featureFlag = FeatureFlag.builder().key("test-feature").active(false).build();

            // WHEN
            boolean result = featureFlagService.isFeatureEnabledForUser(userId, featureFlag);

            // THEN
            assertThat(result).isFalse();
            verify(featureAssignmentRepository, never()).findById(any());
            verify(userAccountRepository, never()).findById(any());
            verify(featureAssignmentRepository, never()).save(any());
            verify(featureAssignmentHistoryRepository, never()).save(any());
        }

        @Test
        void should_disable_feature_if_onlyNotificationForNewUser_and_user_is_old() {
            // GIVEN
            Long userId = 1L;
            String featureKey = "only-new";
            LocalDateTime deploymentDate = LocalDateTime.now().minusDays(10);
            LocalDateTime userCreationDate = LocalDateTime.now().minusDays(20); // Created BEFORE deployment

            FeatureFlag featureFlag = FeatureFlag.builder()
                    .key(featureKey)
                    .active(true)
                    .onlyForNewUser(true)
                    .deploymentDate(deploymentDate)
                    .rolloutPct(50)
                    .build();

            User user = new User() {
            }; // Abstract class
            user.setId(userId);
            user.setCreationDateTime(userCreationDate);

            when(featureAssignmentRepository.findById(any())).thenReturn(Optional.empty());
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userFeatureAssignmentService.saveAssignment(any(), any(), anyBoolean())).thenReturn(false);

            // WHEN
            boolean result = featureFlagService.isFeatureEnabledForUser(userId, featureFlag);

            // THEN
            assertThat(result).isFalse();

            ArgumentCaptor<UserFeatureAssignment> assignmentCaptor = ArgumentCaptor.forClass(UserFeatureAssignment.class);
            ArgumentCaptor<Boolean> fallbackCaptor = ArgumentCaptor.forClass(Boolean.class);

            verify(userFeatureAssignmentService).saveAssignment(assignmentCaptor.capture(), any(), fallbackCaptor.capture());

            UserFeatureAssignment savedAssignment = assignmentCaptor.getValue();
            assertThat(savedAssignment.isEnabled()).isFalse();
            assertThat(savedAssignment.getAssignmentSource()).isEqualTo(FeatureAssignmentSource.PRE_DEPLOYMENT);
            assertThat(savedAssignment.getBucket()).isZero();
            assertThat(fallbackCaptor.getValue()).isFalse();
        }

        @Test
        void should_calculate_rollout_if_not_restricted_to_new_users() {
            // GIVEN
            Long userId = 1L;
            String featureKey = "test-rollout";

            // Setup feature flag with 100% rollout to guarantee true
            FeatureFlag featureFlag = FeatureFlag.builder()
                    .key(featureKey)
                    .active(true)
                    .onlyForNewUser(false)
                    .rolloutPct(100)
                    .build();

            User user = new User() {
            };
            user.setId(userId);

            when(featureAssignmentRepository.findById(any())).thenReturn(Optional.empty());
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userFeatureAssignmentService.saveAssignment(any(), any(), anyBoolean())).thenReturn(true);

            // WHEN
            boolean result = featureFlagService.isFeatureEnabledForUser(userId, featureFlag);

            // THEN
            assertThat(result).isTrue();

            ArgumentCaptor<UserFeatureAssignment> assignmentCaptor = ArgumentCaptor.forClass(UserFeatureAssignment.class);
            ArgumentCaptor<Boolean> fallbackCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(userFeatureAssignmentService).saveAssignment(assignmentCaptor.capture(), any(), fallbackCaptor.capture());

            UserFeatureAssignment savedAssignment = assignmentCaptor.getValue();
            assertThat(savedAssignment.isEnabled()).isTrue();
            assertThat(savedAssignment.getAssignmentSource()).isEqualTo(FeatureAssignmentSource.HASH);
            assertThat(fallbackCaptor.getValue()).isTrue();
        }

        @Test
        void should_exclude_based_on_rollout_pct() {
            // GIVEN
            Long userId = 1L;
            String featureKey = "test-rollout-0";

            // Setup feature flag with 0% rollout to guarantee false
            FeatureFlag featureFlag = FeatureFlag.builder()
                    .key(featureKey)
                    .active(true)
                    .onlyForNewUser(false)
                    .rolloutPct(0)
                    .build();

            User user = new User() {
            };
            user.setId(userId);

            when(featureAssignmentRepository.findById(any())).thenReturn(Optional.empty());
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userFeatureAssignmentService.saveAssignment(any(), any(), anyBoolean())).thenReturn(false);

            // WHEN
            boolean result = featureFlagService.isFeatureEnabledForUser(userId, featureFlag);

            // THEN
            assertThat(result).isFalse();

            ArgumentCaptor<UserFeatureAssignment> assignmentCaptor = ArgumentCaptor.forClass(UserFeatureAssignment.class);
            ArgumentCaptor<Boolean> fallbackCaptor = ArgumentCaptor.forClass(Boolean.class);
            verify(userFeatureAssignmentService).saveAssignment(assignmentCaptor.capture(), any(), fallbackCaptor.capture());

            UserFeatureAssignment savedAssignment = assignmentCaptor.getValue();
            assertThat(savedAssignment.isEnabled()).isFalse();
            assertThat(fallbackCaptor.getValue()).isFalse();
        }

        @Test
        void should_handle_new_user_check_when_user_is_created_after_deployment() {
            // GIVEN
            Long userId = 1L;
            String featureKey = "new-feature";
            LocalDateTime deploymentDate = LocalDateTime.now().minusDays(10);
            LocalDateTime userCreationDate = LocalDateTime.now().minusDays(5); // Created AFTER deployment

            FeatureFlag featureFlag = FeatureFlag.builder()
                    .key(featureKey)
                    .active(true)
                    .onlyForNewUser(true)
                    .deploymentDate(deploymentDate)
                    .rolloutPct(100) // Force enable via rollout
                    .build();

            User user = new User() {
            };
            user.setId(userId);
            user.setCreationDateTime(userCreationDate);

            when(featureAssignmentRepository.findById(any())).thenReturn(Optional.empty());
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userFeatureAssignmentService.saveAssignment(any(), any(), anyBoolean())).thenReturn(true);

            // WHEN
            boolean result = featureFlagService.isFeatureEnabledForUser(userId, featureFlag);

            // THEN
            assertThat(result).isTrue(); // Should pass the "new user" check and fall through to bucket logic

            ArgumentCaptor<UserFeatureAssignment> assignmentCaptor = ArgumentCaptor.forClass(UserFeatureAssignment.class);
            verify(userFeatureAssignmentService).saveAssignment(assignmentCaptor.capture(), any(), anyBoolean());
            assertThat(assignmentCaptor.getValue().getAssignmentSource()).isEqualTo(FeatureAssignmentSource.HASH);
        }

        @Test
        void should_return_false_when_feature_is_active_only_for_new_users_but_deployment_date_is_null() {
            // GIVEN
            Long userId = 1L;
            String featureKey = "new-feature-without-date";

            FeatureFlag featureFlag = FeatureFlag.builder()
                    .key(featureKey)
                    .active(true)
                    .onlyForNewUser(true)
                    .deploymentDate(null)
                    .rolloutPct(100)
                    .build();

            User user = new User() {
            };
            user.setId(userId);
            user.setCreationDateTime(LocalDateTime.now().minusDays(1));

            when(featureAssignmentRepository.findById(any())).thenReturn(Optional.empty());
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));

            // WHEN
            boolean result = featureFlagService.isFeatureEnabledForUser(userId, featureFlag);

            // THEN
            assertThat(result).isFalse();
            verify(userFeatureAssignmentService, never()).saveAssignment(any(), any(), anyBoolean());
            verify(featureAssignmentHistoryRepository, never()).save(any());
            verify(featureAssignmentRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateRolloutForFeatureFlag {
        @Test
        void should_do_nothing_if_value_is_same() {
            FeatureFlag featureFlag = FeatureFlag.builder().rolloutPct(50).build();

            featureFlagService.updateRolloutForFeatureFlag(featureFlag, 50);

            verify(featureFlagRepository, never()).save(any());
            verify(featureAssignmentHistoryRepository, never()).saveHistoryForChangingAssignments(any(), any(), any());
            verify(featureAssignmentRepository, never()).updateRolloutPctAndEnabled(any(), any(), any());
        }

        @Test
        void should_update_rollout_and_history_when_increasing() {
            String key = "test-feature";
            FeatureFlag featureFlag = FeatureFlag.builder().key(key).rolloutPct(10).build();

            featureFlagService.updateRolloutForFeatureFlag(featureFlag, 50);

            assertThat(featureFlag.getRolloutPct()).isEqualTo(50);
            verify(featureFlagRepository).save(featureFlag);
            verify(featureAssignmentHistoryRepository).saveHistoryForChangingAssignments(key, 50, FeatureAssignmentReason.ROLLOUT_INCREASED.name());
            verify(featureAssignmentRepository).updateRolloutPctAndEnabled(key, 50, FeatureAssignmentSource.HASH);
        }

        @Test
        void should_update_rollout_and_history_when_rollback() {
            String key = "test-feature";
            FeatureFlag featureFlag = FeatureFlag.builder().key(key).rolloutPct(80).build();

            featureFlagService.updateRolloutForFeatureFlag(featureFlag, 20);

            assertThat(featureFlag.getRolloutPct()).isEqualTo(20);
            verify(featureFlagRepository).save(featureFlag);
            verify(featureAssignmentHistoryRepository).saveHistoryForChangingAssignments(key, 20, FeatureAssignmentReason.ROLLBACK.name());
            verify(featureAssignmentRepository).updateRolloutPctAndEnabled(key, 20, FeatureAssignmentSource.HASH);
        }
    }

    @Nested
    class ToggleFeatureFlag {
        @Test
        void should_do_nothing_if_status_is_same() {
            FeatureFlag featureFlag = FeatureFlag.builder().active(true).build();

            featureFlagService.toggleFeatureFlag(featureFlag, true);

            verify(featureFlagRepository, never()).save(any());
        }

        @Test
        void should_update_status_and_remove_deployment_date_when_disabling() {
            FeatureFlag featureFlag = FeatureFlag.builder().active(true).build();

            featureFlagService.toggleFeatureFlag(featureFlag, false);

            assertThat(featureFlag.isActive()).isFalse();
            assertThat(featureFlag.getDeploymentDate()).isNull();
            verify(featureFlagRepository).save(featureFlag);
        }

        @Test
        void should_update_status_and_set_deployment_date_when_enabling() {
            FeatureFlag featureFlag = FeatureFlag.builder().active(false).build();

            featureFlagService.toggleFeatureFlag(featureFlag, true);

            assertThat(featureFlag.isActive()).isTrue();
            assertThat(featureFlag.getDeploymentDate()).isNotNull();
            verify(featureFlagRepository).save(featureFlag);
        }
    }

    @Nested
    class IsPartnerOptedIn {

        private static final String KEY = "partner_completed_optin";

        private UserApi partner(String name) {
            return UserApi.builder().id(1L).name(name).build();
        }

        @Test
        void should_return_false_when_partner_is_null_or_has_no_name() {
            assertThat(featureFlagService.isPartnerOptedIn(KEY, null)).isFalse();
            assertThat(featureFlagService.isPartnerOptedIn(KEY, partner(" "))).isFalse();
            verifyNoInteractions(featureFlagRepository);
        }

        @Test
        void should_return_false_when_flag_is_missing() {
            when(featureFlagRepository.findById(KEY)).thenReturn(Optional.empty());

            assertThat(featureFlagService.isPartnerOptedIn(KEY, partner("dfconnect-ics"))).isFalse();
        }

        @Test
        void should_return_false_when_flag_is_inactive_even_if_listed() {
            when(featureFlagRepository.findById(KEY)).thenReturn(Optional.of(
                    FeatureFlag.builder().key(KEY).active(false).optedInPartners("dfconnect-ics").build()));

            assertThat(featureFlagService.isPartnerOptedIn(KEY, partner("dfconnect-ics"))).isFalse();
        }

        @Test
        void should_return_true_only_for_listed_partners_when_active() {
            when(featureFlagRepository.findById(KEY)).thenReturn(Optional.of(
                    FeatureFlag.builder().key(KEY).active(true).optedInPartners(" dfconnect-ics , other-partner,").build()));

            assertThat(featureFlagService.isPartnerOptedIn(KEY, partner("dfconnect-ics"))).isTrue();
            assertThat(featureFlagService.isPartnerOptedIn(KEY, partner("other-partner "))).isTrue();
            assertThat(featureFlagService.isPartnerOptedIn(KEY, partner("Dfconnect-Ics"))).isFalse();
            assertThat(featureFlagService.isPartnerOptedIn(KEY, partner("unknown"))).isFalse();
        }

        @Test
        void should_return_false_when_list_is_empty() {
            when(featureFlagRepository.findById(KEY)).thenReturn(Optional.of(
                    FeatureFlag.builder().key(KEY).active(true).optedInPartners(null).build()));

            assertThat(featureFlagService.isPartnerOptedIn(KEY, partner("dfconnect-ics"))).isFalse();
        }
    }

    @Nested
    class UpdateOptedInPartners {

        @Test
        void should_normalize_and_save_the_list() {
            FeatureFlag featureFlag = FeatureFlag.builder().key("partner_completed_optin").optedInPartners("old").build();

            featureFlagService.updateOptedInPartners(featureFlag, java.util.List.of(" a ", "b", "a", "", "c"));

            assertThat(featureFlag.getOptedInPartners()).isEqualTo("a,b,c");
            verify(featureFlagRepository).save(featureFlag);
        }

        @Test
        void should_clear_the_list_when_empty() {
            FeatureFlag featureFlag = FeatureFlag.builder().key("partner_completed_optin").optedInPartners("a").build();

            featureFlagService.updateOptedInPartners(featureFlag, java.util.List.of());

            assertThat(featureFlag.getOptedInPartners()).isEmpty();
            assertThat(featureFlag.getOptedInPartnerNames()).isEmpty();
            verify(featureFlagRepository).save(featureFlag);
        }

        @Test
        void should_refuse_the_owner_partner() {
            FeatureFlag featureFlag = FeatureFlag.builder().key("partner_completed_optin").optedInPartners("a").build();

            assertThatThrownBy(() -> featureFlagService.updateOptedInPartners(featureFlag, java.util.List.of("a", "dfconnect-proprietaire")))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(featureFlag.getOptedInPartners()).isEqualTo("a");
            verify(featureFlagRepository, never()).save(any());
        }
    }
}

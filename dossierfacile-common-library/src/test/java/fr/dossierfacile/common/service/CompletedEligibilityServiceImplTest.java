package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.CompletedEligibilityService;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompletedEligibilityServiceImplTest {

    @Mock
    private TenantUserApiRepository tenantUserApiRepository;
    @Mock
    private FeatureFlagService featureFlagService;

    @InjectMocks
    private CompletedEligibilityServiceImpl completedEligibilityService;

    private Tenant buildTenant(TenantFileStatus status, ApplicationType applicationType) {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(applicationType)
                .build();
        return Tenant.builder()
                .id(100L)
                .status(status)
                .apartmentSharing(apartmentSharing)
                .build();
    }

    private void mockFullEligibility(Tenant tenant, boolean flagEnabled) {
        when(tenantUserApiRepository.existsByTenant(tenant)).thenReturn(false);
        when(featureFlagService.isFeatureEnabledForUser(tenant.getId(), CompletedEligibilityService.COMPLETED_OPTIN_FEATURE_FLAG))
                .thenReturn(flagEnabled);
    }

    @Nested
    class IsEligibleForOptIn {

        @Test
        void should_be_eligible_for_submitted_alone_dossier_within_rollout() {
            Tenant tenant = buildTenant(TenantFileStatus.TO_PROCESS, ApplicationType.ALONE);
            mockFullEligibility(tenant, true);

            assertThat(completedEligibilityService.isEligibleForOptIn(tenant)).isTrue();
        }

        @Test
        void should_be_eligible_when_dossier_is_already_completed() {
            Tenant tenant = buildTenant(TenantFileStatus.COMPLETED, ApplicationType.ALONE);
            mockFullEligibility(tenant, true);

            assertThat(completedEligibilityService.isEligibleForOptIn(tenant)).isTrue();
        }

        @Test
        void should_be_eligible_when_dossier_has_already_been_reviewed() {
            // On a VALIDATED or DECLINED dossier the choice applies to the next re-submission
            for (TenantFileStatus status : new TenantFileStatus[]{
                    TenantFileStatus.VALIDATED, TenantFileStatus.DECLINED}) {
                Tenant tenant = buildTenant(status, ApplicationType.ALONE);
                mockFullEligibility(tenant, true);

                assertThat(completedEligibilityService.isEligibleForOptIn(tenant)).isTrue();
            }
        }

        @Test
        void should_not_be_eligible_when_dossier_is_not_submitted() {
            for (TenantFileStatus status : new TenantFileStatus[]{
                    TenantFileStatus.INCOMPLETE, TenantFileStatus.ARCHIVED}) {
                Tenant tenant = buildTenant(status, ApplicationType.ALONE);

                assertThat(completedEligibilityService.isEligibleForOptIn(tenant)).isFalse();
            }
            verifyNoInteractions(tenantUserApiRepository, featureFlagService);
        }

        @Test
        void should_not_be_eligible_when_application_is_not_alone() {
            for (ApplicationType applicationType : new ApplicationType[]{ApplicationType.COUPLE, ApplicationType.GROUP}) {
                Tenant tenant = buildTenant(TenantFileStatus.TO_PROCESS, applicationType);

                assertThat(completedEligibilityService.isEligibleForOptIn(tenant)).isFalse();
            }
            verifyNoInteractions(tenantUserApiRepository, featureFlagService);
        }

        @Test
        void should_not_be_eligible_when_linked_to_a_partner_even_a_dangling_one() {
            Tenant tenant = buildTenant(TenantFileStatus.TO_PROCESS, ApplicationType.ALONE);
            when(tenantUserApiRepository.existsByTenant(tenant)).thenReturn(true);

            assertThat(completedEligibilityService.isEligibleForOptIn(tenant)).isFalse();
            // The flag must not be checked (nor assign a bucket) for non-candidates
            verifyNoInteractions(featureFlagService);
        }

        @Test
        void should_not_be_eligible_when_outside_rollout() {
            Tenant tenant = buildTenant(TenantFileStatus.TO_PROCESS, ApplicationType.ALONE);
            mockFullEligibility(tenant, false);

            assertThat(completedEligibilityService.isEligibleForOptIn(tenant)).isFalse();
        }
    }

    @Nested
    class CanBeCompleted {

        @Test
        void should_not_be_completed_when_tenant_requested_a_validation() {
            Tenant tenant = buildTenant(TenantFileStatus.TO_PROCESS, ApplicationType.ALONE);
            tenant.setValidationRequested(true);

            assertThat(completedEligibilityService.canBeCompleted(tenant)).isFalse();
            verifyNoInteractions(tenantUserApiRepository, featureFlagService);
        }

        @Test
        void should_be_completed_when_tenant_never_answered() {
            Tenant tenant = buildTenant(TenantFileStatus.TO_PROCESS, ApplicationType.ALONE);
            tenant.setValidationRequested(null);
            mockFullEligibility(tenant, true);

            assertThat(completedEligibilityService.canBeCompleted(tenant)).isTrue();
        }

        @Test
        void should_be_completed_when_tenant_explicitly_declined_the_validation() {
            Tenant tenant = buildTenant(TenantFileStatus.TO_PROCESS, ApplicationType.ALONE);
            tenant.setValidationRequested(false);
            mockFullEligibility(tenant, true);

            assertThat(completedEligibilityService.canBeCompleted(tenant)).isTrue();
        }
    }
}

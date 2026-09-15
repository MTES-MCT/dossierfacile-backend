package fr.dossierfacile.common.service;

import fr.dossierfacile.common.constants.PartnerConstants;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.LotteryTicket;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LotteryTicketStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.LotteryTicketRepository;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorReviewPolicyImplTest {

    @Mock
    private TenantUserApiRepository tenantUserApiRepository;
    @Mock
    private FeatureFlagService featureFlagService;
    @Mock
    private LotteryTicketRepository lotteryTicketRepository;

    @InjectMocks
    private OperatorReviewPolicyImpl operatorReviewPolicy;

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

    private Tenant buildAloneTenant() {
        return buildTenant(TenantFileStatus.TO_PROCESS, ApplicationType.ALONE);
    }

    private UserApi partner(String name, boolean completedStatusSupported) {
        return UserApi.builder().id((long) name.hashCode()).name(name).completedStatusSupported(completedStatusSupported).build();
    }

    private TenantUserApi linkTo(UserApi partner) {
        return TenantUserApi.builder().userApi(partner).build();
    }

    private void mockPartnerCompletedOptinFlag(boolean enabled) {
        when(featureFlagService.isFeatureEnabled(OperatorReviewPolicy.PARTNER_COMPLETED_OPTIN_FEATURE_FLAG))
                .thenReturn(enabled);
    }

    private void mockNoPartner(Tenant tenant, boolean flagEnabled) {
        when(tenantUserApiRepository.findAllByTenant(tenant)).thenReturn(List.of());
        when(featureFlagService.isFeatureEnabledForUser(tenant.getId(), OperatorReviewPolicy.COMPLETED_OPTIN_FEATURE_FLAG))
                .thenReturn(flagEnabled);
    }

    private void mockLotteryFlag(boolean enabled) {
        when(featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG))
                .thenReturn(enabled);
    }

    private void mockDrawnTicket(Tenant tenant, boolean present) {
        Optional<LotteryTicket> ticket = present
                ? Optional.of(LotteryTicket.builder().tenantId(tenant.getId()).status(LotteryTicketStatus.DRAWN).build())
                : Optional.empty();
        when(lotteryTicketRepository.findFirstByTenantIdAndStatusIn(tenant.getId(), Set.of(LotteryTicketStatus.DRAWN)))
                .thenReturn(ticket);
    }

    @Nested
    class SupportsCompletedStatus {

        @Test
        void should_support_alone_dossier_within_rollout() {
            for (TenantFileStatus status : TenantFileStatus.values()) {
                Tenant tenant = buildTenant(status, ApplicationType.ALONE);
                mockNoPartner(tenant, true);

                assertThat(operatorReviewPolicy.supportsCompletedStatus(tenant)).isTrue();
            }
        }

        @Test
        void should_not_support_when_application_is_not_alone() {
            for (ApplicationType applicationType : new ApplicationType[]{ApplicationType.COUPLE, ApplicationType.GROUP}) {
                Tenant tenant = buildTenant(TenantFileStatus.TO_PROCESS, applicationType);

                assertThat(operatorReviewPolicy.supportsCompletedStatus(tenant)).isFalse();
            }
            verifyNoInteractions(tenantUserApiRepository, featureFlagService);
        }

        @Test
        void should_not_support_when_linked_to_a_partner_that_did_not_opt_in() {
            Tenant tenant = buildAloneTenant();
            when(tenantUserApiRepository.findAllByTenant(tenant))
                    .thenReturn(List.of(linkTo(partner("closed-partner", false))));

            assertThat(operatorReviewPolicy.supportsCompletedStatus(tenant)).isFalse();
            // The opt-in flag must not be checked (nor assign a bucket) for non-candidates
            verify(featureFlagService, never()).isFeatureEnabledForUser(anyLong(), anyString());
        }

        @Test
        void should_support_when_every_linked_partner_opted_in() {
            Tenant tenant = buildAloneTenant();
            when(tenantUserApiRepository.findAllByTenant(tenant))
                    .thenReturn(List.of(linkTo(partner("open-a", true)), linkTo(partner("open-b", true))));
            mockPartnerCompletedOptinFlag(true);
            when(featureFlagService.isFeatureEnabledForUser(tenant.getId(), OperatorReviewPolicy.COMPLETED_OPTIN_FEATURE_FLAG))
                    .thenReturn(true);

            assertThat(operatorReviewPolicy.supportsCompletedStatus(tenant)).isTrue();
        }

        @Test
        void should_not_support_when_one_linked_partner_did_not_opt_in() {
            Tenant tenant = buildAloneTenant();
            when(tenantUserApiRepository.findAllByTenant(tenant))
                    .thenReturn(List.of(linkTo(partner("open-a", true)), linkTo(partner("closed-partner", false))));
            mockPartnerCompletedOptinFlag(true);

            assertThat(operatorReviewPolicy.supportsCompletedStatus(tenant)).isFalse();
            verify(featureFlagService, never()).isFeatureEnabledForUser(anyLong(), anyString());
        }

        @Test
        void should_not_support_when_the_partner_kill_switch_is_off() {
            Tenant tenant = buildAloneTenant();
            when(tenantUserApiRepository.findAllByTenant(tenant))
                    .thenReturn(List.of(linkTo(partner("open-a", true))));
            mockPartnerCompletedOptinFlag(false);

            assertThat(operatorReviewPolicy.supportsCompletedStatus(tenant)).isFalse();
            verify(featureFlagService, never()).isFeatureEnabledForUser(anyLong(), anyString());
        }

        @Test
        void should_not_support_when_outside_rollout() {
            Tenant tenant = buildAloneTenant();
            mockNoPartner(tenant, false);

            assertThat(operatorReviewPolicy.supportsCompletedStatus(tenant)).isFalse();
        }
    }

    @Nested
    class CanRequestOperatorReview {

        @Test
        void should_be_available_on_a_completed_dossier_or_better() {
            for (TenantFileStatus status : new TenantFileStatus[]{
                    TenantFileStatus.TO_PROCESS, TenantFileStatus.COMPLETED, TenantFileStatus.VALIDATED}) {
                Tenant tenant = buildTenant(status, ApplicationType.ALONE);
                mockNoPartner(tenant, true);

                assertThat(operatorReviewPolicy.canRequestOperatorReview(tenant)).isTrue();
            }
        }

        @Test
        void should_be_available_on_a_declined_dossier() {
            // The choice applies to the next re-submission
            Tenant tenant = buildTenant(TenantFileStatus.DECLINED, ApplicationType.ALONE);
            mockNoPartner(tenant, true);

            assertThat(operatorReviewPolicy.canRequestOperatorReview(tenant)).isTrue();
        }

        @Test
        void should_not_be_available_when_dossier_is_not_complete() {
            for (TenantFileStatus status : new TenantFileStatus[]{
                    TenantFileStatus.INCOMPLETE, TenantFileStatus.ARCHIVED}) {
                Tenant tenant = buildTenant(status, ApplicationType.ALONE);

                assertThat(operatorReviewPolicy.canRequestOperatorReview(tenant)).isFalse();
            }
            verifyNoInteractions(tenantUserApiRepository, featureFlagService);
        }

        @Test
        void should_not_be_available_out_of_scope() {
            Tenant tenant = buildTenant(TenantFileStatus.TO_PROCESS, ApplicationType.COUPLE);

            assertThat(operatorReviewPolicy.canRequestOperatorReview(tenant)).isFalse();
        }
    }

    @Nested
    class IsOperatorReviewGranted {

        @Nested
        class LotteryOff {

            @BeforeEach
            void lotteryIsOff() {
                mockLotteryFlag(false);
            }

            @Test
            void should_be_granted_when_tenant_requested_a_validation() {
                Tenant tenant = buildAloneTenant();
                tenant.setValidationRequested(true);

                assertThat(operatorReviewPolicy.isOperatorReviewGranted(tenant)).isTrue();
                verifyNoInteractions(lotteryTicketRepository);
            }

            @Test
            void should_not_be_granted_when_tenant_never_answered_or_declined() {
                for (Boolean answer : new Boolean[]{null, false}) {
                    Tenant tenant = buildAloneTenant();
                    tenant.setValidationRequested(answer);

                    assertThat(operatorReviewPolicy.isOperatorReviewGranted(tenant)).isFalse();
                }
            }
        }

        @Nested
        class LotteryOn {

            @BeforeEach
            void lotteryIsOn() {
                mockLotteryFlag(true);
            }

            @Test
            void should_be_granted_with_a_drawn_ticket() {
                Tenant tenant = buildAloneTenant();
                mockDrawnTicket(tenant, true);

                assertThat(operatorReviewPolicy.isOperatorReviewGranted(tenant)).isTrue();
            }

            @Test
            void should_not_be_granted_without_a_drawn_ticket_even_if_requested() {
                // validationRequested is purely declarative; a PENDING ticket does not count
                Tenant tenant = buildAloneTenant();
                tenant.setValidationRequested(true);
                mockDrawnTicket(tenant, false);

                assertThat(operatorReviewPolicy.isOperatorReviewGranted(tenant)).isFalse();
            }
        }
    }

    @Nested
    class ResolveStatus {

        @Nested
        class LotteryOff {

            @BeforeEach
            void lotteryIsOff() {
                mockLotteryFlag(false);
            }

            @Test
            void should_stay_to_process_when_validation_requested() {
                Tenant tenant = buildAloneTenant();
                tenant.setValidationRequested(true);

                assertThat(operatorReviewPolicy.resolveStatus(tenant, TenantFileStatus.TO_PROCESS)).isEqualTo(TenantFileStatus.TO_PROCESS);
                // Scope is not evaluated (no bucket assignment) for a dossier already in the queue
                verifyNoInteractions(tenantUserApiRepository);
            }

            @Test
            void should_stay_to_process_when_out_of_scope() {
                Tenant tenant = buildTenant(TenantFileStatus.TO_PROCESS, ApplicationType.COUPLE);

                assertThat(operatorReviewPolicy.resolveStatus(tenant, TenantFileStatus.TO_PROCESS)).isEqualTo(TenantFileStatus.TO_PROCESS);
            }

            @Test
            void should_be_completed_when_in_scope_and_no_validation_requested() {
                Tenant tenant = buildAloneTenant();
                tenant.setValidationRequested(false);
                mockNoPartner(tenant, true);

                assertThat(operatorReviewPolicy.resolveStatus(tenant, TenantFileStatus.TO_PROCESS)).isEqualTo(TenantFileStatus.COMPLETED);
            }
        }

        @Nested
        class LotteryOn {

            @BeforeEach
            void lotteryIsOn() {
                mockLotteryFlag(true);
            }

            @Test
            void should_be_completed_while_the_application_is_pending() {
                Tenant tenant = buildAloneTenant();
                tenant.setValidationRequested(true);
                mockDrawnTicket(tenant, false);
                mockNoPartner(tenant, true);

                assertThat(operatorReviewPolicy.resolveStatus(tenant, TenantFileStatus.TO_PROCESS)).isEqualTo(TenantFileStatus.COMPLETED);
            }

            @Test
            void should_stay_to_process_with_a_drawn_ticket() {
                Tenant tenant = buildAloneTenant();
                mockDrawnTicket(tenant, true);

                assertThat(operatorReviewPolicy.resolveStatus(tenant, TenantFileStatus.TO_PROCESS)).isEqualTo(TenantFileStatus.TO_PROCESS);
                verifyNoInteractions(tenantUserApiRepository);
            }
        }
    }

    @Nested
    class IsPartnerOptedIn {

        @Test
        void should_be_opted_in_when_flagged_on_the_partner_and_kill_switch_on() {
            mockPartnerCompletedOptinFlag(true);

            assertThat(operatorReviewPolicy.isPartnerOptedIn(partner("dfconnect-ics", true))).isTrue();
        }

        @Test
        void should_not_be_opted_in_when_kill_switch_off() {
            mockPartnerCompletedOptinFlag(false);

            assertThat(operatorReviewPolicy.isPartnerOptedIn(partner("dfconnect-ics", true))).isFalse();
        }

        @Test
        void should_not_be_opted_in_when_not_flagged_on_the_partner_without_reading_the_flag() {
            assertThat(operatorReviewPolicy.isPartnerOptedIn(partner("dfconnect-ics", false))).isFalse();
            assertThat(operatorReviewPolicy.isPartnerOptedIn(null)).isFalse();
            verifyNoInteractions(featureFlagService);
        }

        @Test
        void should_never_opt_in_the_owner_partner() {
            assertThat(operatorReviewPolicy.isPartnerOptedIn(partner(PartnerConstants.DF_OWNER_NAME, true))).isFalse();
            verifyNoInteractions(featureFlagService);
        }
    }
}

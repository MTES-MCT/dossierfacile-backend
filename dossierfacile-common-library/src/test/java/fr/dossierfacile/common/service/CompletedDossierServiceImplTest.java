package fr.dossierfacile.common.service;

import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.CompletedEligibilityService;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class CompletedDossierServiceImplTest {

    private CompletedEligibilityService completedEligibilityService;
    private TenantCommonRepository tenantCommonRepository;
    private TenantLogCommonService tenantLogCommonService;
    private TenantMapperForMail tenantMapperForMail;
    private MailCommonService mailCommonService;
    private ApartmentSharingCommonService apartmentSharingCommonService;

    private CompletedDossierServiceImpl service;

    private Tenant tenant;
    private ApartmentSharing apartmentSharing;

    @BeforeEach
    void setUp() {
        completedEligibilityService = mock(CompletedEligibilityService.class);
        tenantCommonRepository = mock(TenantCommonRepository.class);
        tenantLogCommonService = mock(TenantLogCommonService.class);
        tenantMapperForMail = mock(TenantMapperForMail.class);
        mailCommonService = mock(MailCommonService.class);
        apartmentSharingCommonService = mock(ApartmentSharingCommonService.class);

        service = new CompletedDossierServiceImpl(
                completedEligibilityService,
                tenantCommonRepository,
                tenantLogCommonService,
                tenantMapperForMail,
                Optional.of(mailCommonService),
                apartmentSharingCommonService
        );

        apartmentSharing = ApartmentSharing.builder().id(300L).build();
        tenant = Tenant.builder().id(100L).apartmentSharing(apartmentSharing).build();
    }

    @Nested
    class ToCompletedIfEligible {

        @Test
        void should_switch_to_completed_when_eligible_and_entering_the_queue() {
            // Given - submission: the persisted status is not TO_PROCESS yet
            tenant.setStatus(TenantFileStatus.INCOMPLETE);
            when(completedEligibilityService.canBeCompleted(tenant)).thenReturn(true);

            assertThat(service.toCompletedIfEligible(tenant, TenantFileStatus.TO_PROCESS))
                    .isEqualTo(TenantFileStatus.COMPLETED);
        }

        // A declined document replaced by the tenant sends the dossier straight back to
        // TO_PROCESS (no new honor declaration): this is an entry into the queue too
        @Test
        void should_switch_to_completed_when_resubmitting_after_a_verdict() {
            for (TenantFileStatus previous : new TenantFileStatus[]{TenantFileStatus.DECLINED, TenantFileStatus.VALIDATED}) {
                tenant.setStatus(previous);
                when(completedEligibilityService.canBeCompleted(tenant)).thenReturn(true);

                assertThat(service.toCompletedIfEligible(tenant, TenantFileStatus.TO_PROCESS))
                        .as("re-entering the queue from %s", previous)
                        .isEqualTo(TenantFileStatus.COMPLETED);
            }
        }

        @Test
        void should_keep_to_process_when_not_eligible() {
            tenant.setStatus(TenantFileStatus.INCOMPLETE);
            when(completedEligibilityService.canBeCompleted(tenant)).thenReturn(false);

            assertThat(service.toCompletedIfEligible(tenant, TenantFileStatus.TO_PROCESS))
                    .isEqualTo(TenantFileStatus.TO_PROCESS);
        }

        // Regression: a dossier that became eligible after its submission (rollout
        // increase) must not leave the queue at the first recomputation, e.g. while
        // an operator deletes a file in the BO
        @Test
        void should_never_take_a_dossier_already_in_the_queue_out_of_it() {
            tenant.setStatus(TenantFileStatus.TO_PROCESS);

            assertThat(service.toCompletedIfEligible(tenant, TenantFileStatus.TO_PROCESS))
                    .isEqualTo(TenantFileStatus.TO_PROCESS);
            verifyNoInteractions(completedEligibilityService);
        }

        @Test
        void should_return_other_statuses_unchanged_without_checking_eligibility() {
            for (TenantFileStatus status : new TenantFileStatus[]{
                    TenantFileStatus.INCOMPLETE, TenantFileStatus.VALIDATED, TenantFileStatus.DECLINED, TenantFileStatus.ARCHIVED}) {
                assertThat(service.toCompletedIfEligible(tenant, status)).isEqualTo(status);
            }
            verifyNoInteractions(completedEligibilityService);
        }
    }

    @Nested
    class SwitchToCompleted {

        @Test
        void should_take_an_eligible_dossier_out_of_the_queue() {
            // Given - the tenant withdrew its verification request
            tenant.setStatus(TenantFileStatus.TO_PROCESS);
            tenant.setValidationRequested(false);
            when(completedEligibilityService.canBeCompleted(tenant)).thenReturn(true);

            // When
            boolean switched = service.switchToCompleted(tenant);

            // Then
            assertThat(switched).isTrue();
            assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.COMPLETED);
            verify(tenantCommonRepository).save(tenant);
            assertThat(apartmentSharing.getLastUpdateDate()).isNotNull();
            verify(apartmentSharingCommonService).save(apartmentSharing);
        }

        @Test
        void should_do_nothing_when_not_eligible() {
            tenant.setStatus(TenantFileStatus.TO_PROCESS);
            when(completedEligibilityService.canBeCompleted(tenant)).thenReturn(false);

            assertThat(service.switchToCompleted(tenant)).isFalse();
            assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
            verify(tenantCommonRepository, never()).save(any(Tenant.class));
        }

        @Test
        void should_do_nothing_when_dossier_is_not_in_the_queue() {
            for (TenantFileStatus status : new TenantFileStatus[]{
                    TenantFileStatus.INCOMPLETE, TenantFileStatus.COMPLETED, TenantFileStatus.VALIDATED, TenantFileStatus.DECLINED}) {
                tenant.setStatus(status);

                assertThat(service.switchToCompleted(tenant)).as("from %s", status).isFalse();
                assertThat(tenant.getStatus()).isEqualTo(status);
            }
            verifyNoInteractions(completedEligibilityService);
            verify(tenantCommonRepository, never()).save(any(Tenant.class));
        }
    }

    @Nested
    class SwitchBackToProcessing {

        @Test
        void should_switch_completed_dossier_back_to_processing() {
            // Given - a COMPLETED dossier must never be exposed to partners
            tenant.setStatus(TenantFileStatus.COMPLETED);
            tenant.setValidationRequested(null);
            UserApi userApi = UserApi.builder().id(200L).name("partner-client").name2("Demo partenaire").build();
            TenantDto tenantDto = mock(TenantDto.class);
            when(tenantMapperForMail.toDto(tenant)).thenReturn(tenantDto);
            TransactionSynchronizationManager.initSynchronization();

            try {
                // When
                service.switchBackToProcessing(tenant, userApi);

                // Then - back to the operator queue, positioned at switch time
                assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
                assertThat(tenant.getLastUpdateDate()).isNotNull();
                // The user's explicit choice is left untouched
                assertThat(tenant.getValidationRequested()).isNull();
                verify(tenantCommonRepository).save(tenant);
                verify(tenantLogCommonService).saveTenantLog(argThat(log ->
                        log.getLogType() == LogType.COMPLETED_SWITCHED_TO_PROCESS && log.getTenantId().equals(tenant.getId())));
                // The full PDF rendered with the COMPLETED design is dropped
                verify(apartmentSharingCommonService).resetDossierPdfGenerated(apartmentSharing);

                // And - the mail mentioning the partner is sent after commit
                verify(mailCommonService, never()).sendEmailCompletedSwitchedToProcessing(any(), any());
                TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
                verify(mailCommonService).sendEmailCompletedSwitchedToProcessing(tenantDto, "Demo partenaire");
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        void should_switch_without_mail_when_no_partner_context() {
            // Given - BO rollback: no partner involved
            tenant.setStatus(TenantFileStatus.COMPLETED);

            // When
            service.switchBackToProcessing(tenant, null);

            // Then - the switch happens but no mail is sent (the template mentions a partner)
            assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
            verify(tenantCommonRepository).save(tenant);
            verify(tenantLogCommonService).saveTenantLog(any(TenantLog.class));
            verify(apartmentSharingCommonService).resetDossierPdfGenerated(apartmentSharing);
            verify(mailCommonService, never()).sendEmailCompletedSwitchedToProcessing(any(), any());
        }

        @Test
        void should_do_nothing_when_dossier_is_not_completed() {
            tenant.setStatus(TenantFileStatus.VALIDATED);

            service.switchBackToProcessing(tenant, UserApi.builder().id(200L).build());

            assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.VALIDATED);
            verify(tenantCommonRepository, never()).save(any(Tenant.class));
            verify(tenantLogCommonService, never()).saveTenantLog(any(TenantLog.class));
            verify(apartmentSharingCommonService, never()).resetDossierPdfGenerated(any());
            verify(mailCommonService, never()).sendEmailCompletedSwitchedToProcessing(any(), any());
        }
    }
}

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
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
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

    private TenantCommonRepository tenantCommonRepository;
    private TenantLogCommonService tenantLogCommonService;
    private TenantMapperForMail tenantMapperForMail;
    private MailCommonService mailCommonService;
    private ApartmentSharingCommonService apartmentSharingCommonService;
    private LotteryTicketService lotteryTicketService;

    private CompletedDossierServiceImpl service;

    private Tenant tenant;
    private ApartmentSharing apartmentSharing;

    @BeforeEach
    void setUp() {
        tenantCommonRepository = mock(TenantCommonRepository.class);
        tenantLogCommonService = mock(TenantLogCommonService.class);
        tenantMapperForMail = mock(TenantMapperForMail.class);
        mailCommonService = mock(MailCommonService.class);
        apartmentSharingCommonService = mock(ApartmentSharingCommonService.class);
        lotteryTicketService = mock(LotteryTicketService.class);

        service = new CompletedDossierServiceImpl(
                tenantCommonRepository,
                tenantLogCommonService,
                tenantMapperForMail,
                Optional.of(mailCommonService),
                apartmentSharingCommonService,
                lotteryTicketService
        );

        apartmentSharing = ApartmentSharing.builder().id(300L).build();
        tenant = Tenant.builder().id(100L).apartmentSharing(apartmentSharing).build();
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

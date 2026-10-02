package fr.dossierfacile.scheduler.tasks.completedoptin;

import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationReminderServiceTest {

    private static final long TENANT_ID = 42L;

    @Mock
    private CompletedOptinTenantRepository tenantRepository;
    @Mock
    private LogService logService;
    @Mock
    private TenantMapperForMail tenantMapperForMail;
    @Mock
    private MailCommonService mailCommonService;
    @InjectMocks
    private ValidationReminderService validationReminderService;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void should_log_the_reminder_then_send_the_mail_after_commit() {
        Tenant tenant = tenant(TenantFileStatus.COMPLETED, null, "tenant@example.com");
        TenantDto tenantDto = new TenantDto();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantMapperForMail.toDto(tenant)).thenReturn(tenantDto);

        boolean reminded = validationReminderService.sendReminder(TENANT_ID);

        assertThat(reminded).isTrue();
        verify(logService).saveLog(LogType.VALIDATION_REMINDER_SENT, TENANT_ID);
        // The mail only leaves once the log is committed
        verify(mailCommonService, never()).sendEmailValidationReminder(tenantDto);
        commit();
        InOrder inOrder = inOrder(logService, mailCommonService);
        inOrder.verify(logService).saveLog(LogType.VALIDATION_REMINDER_SENT, TENANT_ID);
        inOrder.verify(mailCommonService).sendEmailValidationReminder(tenantDto);
    }

    @Test
    void should_not_remind_a_tenant_who_left_the_completed_status() {
        when(tenantRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(tenant(TenantFileStatus.INCOMPLETE, null, "tenant@example.com")));

        assertNotReminded();
    }

    @Test
    void should_not_remind_a_tenant_who_asked_for_the_verification() {
        when(tenantRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(tenant(TenantFileStatus.COMPLETED, true, "tenant@example.com")));

        assertNotReminded();
    }

    @Test
    void should_not_remind_a_tenant_who_declined_the_verification() {
        when(tenantRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(tenant(TenantFileStatus.COMPLETED, false, "tenant@example.com")));

        assertNotReminded();
    }

    @Test
    void should_not_remind_a_deleted_tenant() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertNotReminded();
    }

    private void assertNotReminded() {
        boolean reminded = validationReminderService.sendReminder(TENANT_ID);
        commit();

        assertThat(reminded).isFalse();
        verifyNoInteractions(logService, mailCommonService);
    }

    private static void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private static Tenant tenant(TenantFileStatus status, Boolean validationRequested, String email) {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setStatus(status);
        tenant.setValidationRequested(validationRequested);
        tenant.setEmail(email);
        return tenant;
    }
}

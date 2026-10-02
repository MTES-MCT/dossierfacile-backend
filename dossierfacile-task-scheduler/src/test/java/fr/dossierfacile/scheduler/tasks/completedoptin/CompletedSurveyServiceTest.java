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
class CompletedSurveyServiceTest {

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
    private CompletedSurveyService completedSurveyService;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void should_log_the_survey_then_send_the_mail_after_commit() {
        Tenant tenant = tenant(TenantFileStatus.COMPLETED, "tenant@example.com");
        TenantDto tenantDto = new TenantDto();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantMapperForMail.toDto(tenant)).thenReturn(tenantDto);

        boolean sent = completedSurveyService.sendSurvey(TENANT_ID);

        assertThat(sent).isTrue();
        verify(logService).saveLog(LogType.COMPLETED_SURVEY_SENT, TENANT_ID);
        // The mail only leaves once the log is committed
        verify(mailCommonService, never()).sendEmailCompletedSurvey(tenantDto);
        commit();
        InOrder inOrder = inOrder(logService, mailCommonService);
        inOrder.verify(logService).saveLog(LogType.COMPLETED_SURVEY_SENT, TENANT_ID);
        inOrder.verify(mailCommonService).sendEmailCompletedSurvey(tenantDto);
    }

    @Test
    void should_send_the_survey_whatever_the_answer_to_the_verification_question() {
        Tenant tenant = tenant(TenantFileStatus.COMPLETED, "tenant@example.com");
        // Declined (or cancelled) the verification: the dossier is still COMPLETED
        tenant.setValidationRequested(false);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        assertThat(completedSurveyService.sendSurvey(TENANT_ID)).isTrue();
    }

    @Test
    void should_not_survey_a_tenant_who_left_the_completed_status() {
        when(tenantRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(tenant(TenantFileStatus.VALIDATED, "tenant@example.com")));

        assertNotSurveyed();
    }

    @Test
    void should_not_survey_a_deleted_tenant() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertNotSurveyed();
    }

    private void assertNotSurveyed() {
        boolean sent = completedSurveyService.sendSurvey(TENANT_ID);
        commit();

        assertThat(sent).isFalse();
        verifyNoInteractions(logService, mailCommonService);
    }

    private static void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private static Tenant tenant(TenantFileStatus status, String email) {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setStatus(status);
        tenant.setEmail(email);
        return tenant;
    }
}

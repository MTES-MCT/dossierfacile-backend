package fr.dossierfacile.scheduler.tasks.completedoptin;

import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.common.utils.TransactionalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationReminderService {

    private final CompletedOptinTenantRepository tenantRepository;
    private final LogService logService;
    private final TenantMapperForMail tenantMapperForMail;
    private final MailCommonService mailCommonService;

    /**
     * Reminds a COMPLETED tenant that the dossier can be verified by an operator.
     * The VALIDATION_REMINDER_SENT log is what prevents a second reminder: it is written
     * before the mail is sent, so a tenant is never reminded twice, even if the task is re-run.
     *
     * @return true if the reminder was sent
     */
    @Transactional
    public boolean sendReminder(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        // The tenant may have answered the question or left COMPLETED since the selection
        if (tenant == null
                || tenant.getStatus() != TenantFileStatus.COMPLETED
                || tenant.getValidationRequested() != null
                || isBlank(tenant.getEmail())) {
            return false;
        }
        logService.saveLog(LogType.VALIDATION_REMINDER_SENT, tenant.getId());
        TenantDto tenantDto = tenantMapperForMail.toDto(tenant);
        TransactionalUtil.afterCommit(() -> mailCommonService.sendEmailValidationReminder(tenantDto));
        return true;
    }
}

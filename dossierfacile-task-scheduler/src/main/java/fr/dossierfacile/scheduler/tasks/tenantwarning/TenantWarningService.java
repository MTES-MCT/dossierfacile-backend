package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class TenantWarningService {

    private final LogService logService;
    private final WarningMailSender mailSender;
    private final ConfirmationTokenService confirmationTokenService;
    private final TenantCommonRepository tenantRepository;

    /**
     * Sends the first inactivity warning email and increments the tenant's warning counter to 1.
     */
    @Transactional
    public void sendFirstWarning(Tenant t) {
        Tenant tenant = tenantRepository.findById(t.getId()).orElse(null);
        if (tenant == null) {
            return;
        }
        mailSender.sendEmailFirstWarningForDeletionOfDocuments(tenant, confirmationTokenService.createToken(tenant));
        tenant.setWarnings(1);
        tenantRepository.save(tenant);
        logService.saveLog(LogType.FIRST_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, tenant.getId());
    }

    /**
     * Sends the second inactivity warning email and increments the tenant's warning counter to 2.
     */
    @Transactional
    public void sendSecondWarning(Tenant t) {
        Tenant tenant = tenantRepository.findById(t.getId()).orElse(null);
        if (tenant == null) {
            return;
        }
        mailSender.sendEmailSecondWarningForDeletionOfDocuments(tenant, confirmationTokenService.createToken(tenant));
        tenant.setWarnings(2);
        tenantRepository.save(tenant);
        logService.saveLog(LogType.SECOND_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, tenant.getId());
    }
}

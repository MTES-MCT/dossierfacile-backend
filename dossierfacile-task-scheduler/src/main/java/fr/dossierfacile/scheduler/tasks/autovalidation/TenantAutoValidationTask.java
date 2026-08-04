package fr.dossierfacile.scheduler.tasks.autovalidation;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.service.interfaces.TenantAutoValidationService;
import fr.dossierfacile.scheduler.tasks.AbstractTask;
import fr.dossierfacile.scheduler.tasks.TaskName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantAutoValidationTask extends AbstractTask {

    private final TenantAutoValidationService tenantAutoValidationService;

    @Value("${tenant.auto.validation.tenant-min-age-minutes:30}")
    private int tenantMinAgeInMinutes;

    @Scheduled(fixedDelayString = "${tenant.auto.validation.task.fixed-delay-ms:300000}", initialDelay = 0)
    public void processTenantAutoValidation() {
        super.startTask(TaskName.TENANT_AUTO_VALIDATION);
        try {
            LocalDateTime maxLastUpdateDate = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(tenantMinAgeInMinutes);
            log.info("Starting tenant auto validation task for tenants flagged since more than {} minutes (lastUpdateDate <= {})", tenantMinAgeInMinutes, maxLastUpdateDate);

            List<Tenant> tenantsToValidate = tenantAutoValidationService.listTenantsToAutoValidate(maxLastUpdateDate);
            countTenantIdForLogging(tenantsToValidate.stream().map(Tenant::getId).toList());

            log.info("Found {} tenants ready for auto-validation", tenantsToValidate.size());

            for (Tenant tenant : tenantsToValidate) {
                processSingleTenantAutoValidation(tenant);
            }

        } catch (Exception e) {
            log.error("Error during tenant auto validation task execution", e);
        } finally {
            super.endTask();
        }
    }

    private void processSingleTenantAutoValidation(Tenant tenant) {
        try {
            tenantAutoValidationService.processAutoValidationForTenant(tenant.getId());
        } catch (Exception e) {
            log.error("Error processing auto-validation for tenant ID [{}]", tenant.getId(), e);
        }
    }
}

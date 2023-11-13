package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.scheduler.LoggingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_DELETION;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDeletionTask {

    @Value("${months_for_deletion_of_archived_tenants:12}")
    private Integer monthsForDeletionOfTenants;
    private final TenantRepository tenantRepository;
    private final TenantWarningService tenantWarningService;

    @Scheduled(cron = "${cron.account-deletion}")
    private void deleteOldAccounts() {
        LoggingContext.startTask(TENANT_DELETION);
        List<Long> tenantIdsToDelete = getTenantsToDelete();
        tenantIdsToDelete.forEach(this::deleteTenant);
        log.info("Deleted {} inactive old tenants", tenantIdsToDelete.size());
        LoggingContext.endTask();
    }

    private List<Long> getTenantsToDelete() {
        LocalDateTime limitDate = LocalDateTime.now().minusMonths(monthsForDeletionOfTenants);
        PageRequest pageRequest = PageRequest.of(0, 5000);
        return tenantRepository.findByLastUpdateDate(limitDate, pageRequest);
    }

    private void deleteTenant(Long tenantId) {
        try {
            tenantWarningService.deleteOldArchivedWarning(tenantId);
        } catch (Exception e) {
            log.error("Error while deleting tenant {}", tenantId, e);
        }
    }

}

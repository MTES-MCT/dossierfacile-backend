package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.scheduler.LoggingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_DELETION;
import static java.time.LocalDateTime.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDeletionTask {

    private static final int PAGE_SIZE = 1000;

    @Value("${months_for_deletion_of_archived_tenants:12}")
    private Integer monthsForDeletionOfTenants;
    private final TenantRepository tenantRepository;
    private final TenantWarningService tenantWarningService;

    @Scheduled(cron = "${cron.account-deletion}")
    private void deleteOldAccounts() {
        LoggingContext.startTask(TENANT_DELETION);

        LocalDateTime limitDate = now().minusMonths(monthsForDeletionOfTenants);
        deleteTenantsNotActiveSince(limitDate);

        LoggingContext.endTask();
    }

    private void deleteTenantsNotActiveSince(LocalDateTime limitDate) {
        Page<Long> toDelete = tenantRepository.findByLastUpdateDate(limitDate, PageRequest.of(0, PAGE_SIZE));
        log.info("Found {} inactive tenants to delete ({} months old)", toDelete.getTotalElements(), monthsForDeletionOfTenants);

        deleteAllTenants(toDelete);

        while (toDelete.hasNext()) {
            toDelete = tenantRepository.findByLastUpdateDate(limitDate, toDelete.nextPageable());
            deleteAllTenants(toDelete);
        }
    }

    private void deleteAllTenants(Page<Long> tenantIds) {
        tenantIds.forEach(this::deleteTenant);
    }

    private void deleteTenant(Long tenantId) {
        try {
            tenantWarningService.deleteOldArchivedWarning(tenantId);
        } catch (Exception e) {
            log.error("Error while deleting tenant {}", tenantId, e);
        }
    }

}

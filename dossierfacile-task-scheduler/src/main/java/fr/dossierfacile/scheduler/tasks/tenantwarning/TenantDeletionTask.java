package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.scheduler.tasks.AbstractTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_DELETION;
import static java.time.LocalDateTime.now;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantDeletionTask extends AbstractTask {

    private static final int PAGE_SIZE = 1000;

    @Value("${months_for_deletion_of_archived_tenants:12}")
    private Integer monthsForDeletionOfTenants;
    private final TenantRepository tenantRepository;
    private final TenantWarningService tenantWarningService;

    @Scheduled(cron = "${cron.account-deletion}")
    private void deleteOldAccounts() {
        super.startTask(TENANT_DELETION);

        try {
            LocalDateTime limitDate = now().minusMonths(monthsForDeletionOfTenants);
            deleteTenantsNotActiveSince(limitDate);
        } catch (Exception e) {
            log.error("Error during tenant deletion task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    private void deleteTenantsNotActiveSince(LocalDateTime limitDate) {
        List<Long> listOfTenantIds = new ArrayList<>();
        Page<Long> toDelete = tenantRepository.findByLastUpdateDate(limitDate, PageRequest.of(0, PAGE_SIZE));
        log.info("Found {} inactive tenants to delete ({} months old)", toDelete.getTotalElements(), monthsForDeletionOfTenants);

        deleteAllTenants(toDelete, listOfTenantIds);

        while (toDelete.hasNext()) {
            toDelete = tenantRepository.findByLastUpdateDate(limitDate, toDelete.nextPageable());
            deleteAllTenants(toDelete, listOfTenantIds);
        }
        addTenantIdsToDeleteForLogging(listOfTenantIds);
    }

    private void deleteAllTenants(Page<Long> tenantIds, List<Long> listOfTenantIds) {
        listOfTenantIds.addAll(tenantIds.getContent());
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

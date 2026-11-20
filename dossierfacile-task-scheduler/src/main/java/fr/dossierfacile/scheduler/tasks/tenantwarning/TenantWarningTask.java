package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.scheduler.tasks.AbstractTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_ARCHIVING;
import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_WARNINGS_1;
import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_WARNINGS_2;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantWarningTask extends AbstractTask {

    static final int PAGE_SIZE = 100;

    @Value("${days_for_deletion_of_documents:45}")
    private Integer daysForDeletionOfDocuments;
    @Value("${days_for_first_warning_deletion:30}")
    private Integer daysForFirstWarningDeletion;
    @Value("${days_for_second_warning_deletion:37}")
    private Integer daysForSecondWarningDeletion;
    private final TenantRepository tenantRepository;
    private final TenantWarningService tenantWarningService;
    private final TenantArchivingService tenantArchivingService;

    @Scheduled(cron = "${cron.process.warnings}")
    public void accountWarningsForDocumentDeletion() {
        // Archive accounts after 45 days of inactivity (tenants with warnings=2)
        LocalDateTime limitDateForArchiving = LocalDateTime.now().minusDays(daysForDeletionOfDocuments);
        archiveInactiveTenants(limitDateForArchiving);

        // Send second warning after 37 days of inactivity (tenants with warnings=1)
        LocalDateTime limitDateForSecondWarning = LocalDateTime.now().minusDays(daysForSecondWarningDeletion);
        processSecondWarnings(limitDateForSecondWarning);

        // Send first warning after 30 days of inactivity (tenants with warnings=0)
        LocalDateTime limitDateForFirstWarning = LocalDateTime.now().minusDays(daysForFirstWarningDeletion);
        processFirstWarnings(limitDateForFirstWarning);
    }

    private void archiveInactiveTenants(LocalDateTime limitDate) {
        super.startTask(TENANT_ARCHIVING);
        try {
            List<Long> tenantIds = new ArrayList<>();

            // Path 1: tenants with documents who went through both warnings
            log.info("Found {} tenants with documents to archive",
                    tenantRepository.countInactiveTenantsWithDocuments(limitDate, 2));
            processTenants(
                    beforeId -> tenantRepository.findInactiveTenantsWithDocuments(limitDate, 2, beforeId, batch()),
                    this::tryArchiveTenant,
                    tenantIds);

            // Path 2: tenants without documents — archived directly without prior warnings
            log.info("Found {} tenants without documents to archive silently",
                    tenantRepository.countInactiveTenantsWithoutDocuments(limitDate));
            processTenants(
                    beforeId -> tenantRepository.findInactiveTenantsWithoutDocuments(limitDate, beforeId, batch()),
                    this::tryArchiveTenant,
                    tenantIds);

            countTenantIdForLogging(tenantIds);
        } catch (Exception e) {
            log.error("Error during tenant archiving task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    private void processSecondWarnings(LocalDateTime limitDate) {
        super.startTask(TENANT_WARNINGS_2);
        try {
            log.info("Found {} tenants who will be warned for the SECOND time by email",
                    tenantRepository.countInactiveTenantsWithDocuments(limitDate, 1));
            processWarnings(limitDate, 1, tenantWarningService::sendSecondWarning);
        } catch (Exception e) {
            log.error("Error during tenant second warning task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    private void processFirstWarnings(LocalDateTime limitDate) {
        super.startTask(TENANT_WARNINGS_1);
        try {
            log.info("Found {} tenants who will be warned for the FIRST time by email",
                    tenantRepository.countInactiveTenantsWithDocuments(limitDate, 0));
            processWarnings(limitDate, 0, tenantWarningService::sendFirstWarning);
        } catch (Exception e) {
            log.error("Error during tenant first warning task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    /**
     * Sends the warning to every eligible tenant.
     * A transient Brevo failure only logs a WARN: the tenant is not marked as warned and is retried on the next run.
     * The run logs an ERROR (and therefore alerts) only when every email failed, i.e. Brevo looks down:
     * otherwise a lasting outage would go unnoticed while no tenant is warned nor archived anymore.
     */
    private void processWarnings(LocalDateTime limitDate, int currentWarnings, Consumer<Tenant> warningAction) {
        List<Long> tenantIds = new ArrayList<>();
        AtomicInteger sentMails = new AtomicInteger();
        AtomicInteger failedMails = new AtomicInteger();

        processTenants(
                beforeId -> tenantRepository.findInactiveTenantsWithDocuments(limitDate, currentWarnings, beforeId, batch()),
                tenant -> tryHandleWarning(tenant, warningAction, sentMails, failedMails),
                tenantIds);

        countTenantIdForLogging(tenantIds);
        if (failedMails.get() > 0 && sentMails.get() == 0) {
            log.error("Brevo looks unavailable: all {} warning emails failed, these tenants will be warned on the next run",
                    failedMails.get());
        } else if (failedMails.get() > 0) {
            log.warn("{} warning emails failed transiently and will be retried on the next run", failedMails.get());
        }
    }

    /**
     * Iterates over all tenants returned by the batch query, using keyset pagination on the id
     * so that tenants leaving the result set once processed do not shift the next batches.
     */
    private void processTenants(Function<Long, List<Tenant>> nextBatchBeforeId,
            Consumer<Tenant> action,
            List<Long> ids) {
        long beforeId = Long.MAX_VALUE;
        while (true) {
            List<Tenant> batch = nextBatchBeforeId.apply(beforeId);
            batch.forEach(tenant -> {
                ids.add(tenant.getId());
                action.accept(tenant);
            });
            if (batch.size() < PAGE_SIZE) {
                return;
            }
            beforeId = batch.getLast().getId();
        }
    }

    private void tryArchiveTenant(Tenant tenant) {
        try {
            tenantArchivingService.archiveTenant(tenant);
        } catch (Exception e) {
            log.error("Error while archiving tenant [{}]: {}", tenant.getId(), e.getMessage(), e);
        }
    }

    private void tryHandleWarning(Tenant tenant, Consumer<Tenant> warningAction,
            AtomicInteger sentMails, AtomicInteger failedMails) {
        try {
            warningAction.accept(tenant);
            sentMails.incrementAndGet();
        } catch (TransientMailDeliveryException e) {
            failedMails.incrementAndGet();
            log.warn("Warning email not sent to tenant [{}], it will be retried on the next run: {}",
                    tenant.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Error while processing warning for tenant [{}]: {}", tenant.getId(), e.getMessage(), e);
        }
    }

    private static Pageable batch() {
        return PageRequest.of(0, PAGE_SIZE);
    }
}

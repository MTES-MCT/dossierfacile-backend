package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.scheduler.tasks.AbstractTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_ARCHIVING;
import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_WARNINGS_1;
import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_WARNINGS_2;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantWarningTask extends AbstractTask {

    private static final int PAGE_SIZE = 100;

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
            Page<Tenant> firstPage = tenantRepository.findInactiveTenantsWithDocuments(
                    pageRequest(), limitDate, 2);
            log.info("Found {} tenants with documents to archive", firstPage.getTotalElements());
            processTenants(firstPage,
                    pageable -> tenantRepository.findInactiveTenantsWithDocuments(pageable, limitDate, 2),
                    this::tryArchiveTenant,
                    tenantIds);

            // Path 2: tenants without documents — archived directly without prior warnings
            Page<Tenant> firstPageNoDocs = tenantRepository.findInactiveTenantsWithoutDocuments(
                    pageRequest(), limitDate);
            log.info("Found {} tenants without documents to archive silently", firstPageNoDocs.getTotalElements());
            processTenants(firstPageNoDocs,
                    pageable -> tenantRepository.findInactiveTenantsWithoutDocuments(pageable, limitDate),
                    this::tryArchiveTenant,
                    tenantIds);

            addTenantIdListForLogging(tenantIds);
        } catch (Exception e) {
            log.error("Error during tenant archiving task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    private void processSecondWarnings(LocalDateTime limitDate) {
        super.startTask(TENANT_WARNINGS_2);
        try {
            List<Long> tenantIds = new ArrayList<>();
            Page<Tenant> firstPage = tenantRepository.findInactiveTenantsWithDocuments(
                    pageRequest(), limitDate, 1);
            log.info("Found {} tenants who will be warned for the SECOND time by email", firstPage.getTotalElements());

            processTenants(firstPage,
                    pageable -> tenantRepository.findInactiveTenantsWithDocuments(pageable, limitDate, 1),
                    t -> tryHandleWarning(t, tenantWarningService::sendSecondWarning),
                    tenantIds);

            addTenantIdListForLogging(tenantIds);
        } catch (Exception e) {
            log.error("Error during tenant second warning task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    private void processFirstWarnings(LocalDateTime limitDate) {
        super.startTask(TENANT_WARNINGS_1);
        try {
            List<Long> tenantIds = new ArrayList<>();
            Page<Tenant> firstPage = tenantRepository.findInactiveTenantsWithDocuments(
                    pageRequest(), limitDate, 0);
            log.info("Found {} tenants who will be warned for the FIRST time by email", firstPage.getTotalElements());

            processTenants(firstPage,
                    pageable -> tenantRepository.findInactiveTenantsWithDocuments(pageable, limitDate, 0),
                    t -> tryHandleWarning(t, tenantWarningService::sendFirstWarning),
                    tenantIds);

            addTenantIdListForLogging(tenantIds);
        } catch (Exception e) {
            log.error("Error during tenant first warning task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    /**
     * Iterates over all pages of tenants, filters those with a non-blank email,
     * collects their IDs and applies the given action to each one.
     */
    private void processTenants(Page<Tenant> firstPage,
                                          Function<Pageable, Page<Tenant>> nextPageFn,
                                          Consumer<Tenant> action,
                                          List<Long> ids) {
        Page<Tenant> currentPage = firstPage;
        while (true) {
            currentPage.stream()
                    .forEach(tenant -> {
                        ids.add(tenant.getId());
                        action.accept(tenant);
                    });
            if (!currentPage.hasNext()) {
                break;
            }
            currentPage = nextPageFn.apply(currentPage.nextPageable());
        }
    }

    private void tryArchiveTenant(Tenant tenant) {
        try {
            tenantArchivingService.archiveTenant(tenant);
        } catch (Exception e) {
            log.error("Error while archiving tenant [{}]: {}", tenant.getId(), e.getMessage(), e);
        }
    }

    private void tryHandleWarning(Tenant tenant, Consumer<Tenant> warningAction) {
        try {
            warningAction.accept(tenant);
        } catch (Exception e) {
            log.error("Error while processing warning for tenant [{}]: {}", tenant.getId(), e.getMessage(), e);
        }
    }

    private Pageable pageRequest() {
        return PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "id");
    }
}

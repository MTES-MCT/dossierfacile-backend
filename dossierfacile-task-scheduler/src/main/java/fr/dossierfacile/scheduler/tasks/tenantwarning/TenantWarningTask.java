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

import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_ARCHIVING;
import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_WARNINGS_1;
import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_WARNINGS_2;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

    @Scheduled(cron = "${cron.process.warnings}")
    public void accountWarningsForDocumentDeletion() {
        // Delete documents after 45 days of inactivity (tenants with warnings=2)
        LocalDateTime limitDateForDeletion = LocalDateTime.now().minusDays(daysForDeletionOfDocuments);
        archiveAccounts(limitDateForDeletion);

        // Send second warning after 37 days of inactivity (tenants with warnings=1)
        LocalDateTime limitDateForSecondWarning = LocalDateTime.now().minusDays(daysForSecondWarningDeletion);
        sendSecondWarningMails(limitDateForSecondWarning);

        // Send first warning after 30 days of inactivity (tenants with warnings=0)
        LocalDateTime limitDateForFirstWarning = LocalDateTime.now().minusDays(daysForFirstWarningDeletion);
        sendFirstWarningMails(limitDateForFirstWarning);
    }

    private void archiveAccounts(LocalDateTime limitDate) {
        super.startTask(TENANT_ARCHIVING);
        try {
            processAllWarnings(limitDate, 2);
            archiveCotenantAccounts();
        } catch (Exception e) {
            log.error("Error during tenant archiving task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    private void sendSecondWarningMails(LocalDateTime limitDate) {
        super.startTask(TENANT_WARNINGS_2);
        try {
            processAllWarnings(limitDate, 1);
        } catch (Exception e) {
            log.error("Error during tenant second warning task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    private void sendFirstWarningMails(LocalDateTime limitDate) {
        super.startTask(TENANT_WARNINGS_1);
        try {
            processAllWarnings(limitDate, 0);
        } catch (Exception e) {
            log.error("Error during tenant first warning task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    private void processAllWarnings(LocalDateTime localDateTime, int warnings) {
        List<Long> tenantIds = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "id");
        Page<Tenant> tenantPage = tenantRepository.findByLastLoginDateIsBeforeAndHasDocuments(pageable, localDateTime, warnings);
        switch (warnings) {
            case 0 ->
                    log.info("Found {} tenants who will be warned for FIRST time by email", tenantPage.getTotalElements());
            case 1 ->
                    log.info("Found {} tenants who will be warned for SECOND time by email", tenantPage.getTotalElements());
            case 2 -> log.info("Found {} tenants whose documents will be deleted", tenantPage.getTotalElements());
        }

        processWarningsForPage(warnings, tenantPage, tenantIds);

        while (tenantPage.hasNext()) {
            tenantPage = tenantRepository.findByLastLoginDateIsBeforeAndHasDocuments(tenantPage.nextPageable(), localDateTime, warnings);
            processWarningsForPage(warnings, tenantPage, tenantIds);
        }

        addTenantIdsToDeleteForLogging(tenantIds);
    }

    private void processWarningsForPage(int warnings, Page<Tenant> tenantPage, List<Long> tenantIds) {
        tenantPage.stream()
                .filter(tenant -> isNotBlank(tenant.getEmail()))
                .forEach(t -> {
                    tenantIds.add(t.getId());
                    tryHandlingTenantWarning(warnings, t);
                });
    }

    private void archiveCotenantAccounts() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "id");
        Page<Tenant> tenantPage = tenantRepository.findCotenantsWithNoEmailAndArchivedMainTenant(pageable);

        tenantPage.stream().forEach(t -> tryHandlingTenantWarning(2, t));

        while (tenantPage.hasNext()) {
            tenantPage = tenantRepository.findCotenantsWithNoEmailAndArchivedMainTenant(tenantPage.nextPageable());
            tenantPage.stream().forEach(t -> tryHandlingTenantWarning(2, t));
        }
    }

    private void tryHandlingTenantWarning(int warnings, Tenant t) {
        try {
            tenantWarningService.handleTenantWarning(t, warnings);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}

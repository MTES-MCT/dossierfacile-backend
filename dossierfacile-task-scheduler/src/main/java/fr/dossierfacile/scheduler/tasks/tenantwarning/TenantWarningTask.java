package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.scheduler.LoggingContext;
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

import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_ARCHIVING;
import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_WARNINGS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantWarningTask {

    private static final int PAGE_SIZE = 100;

    @Value("${months_for_deletion_of_documents:3}")
    private Integer monthsForDeletionOfDocuments;
    private final TenantRepository tenantRepository;
    private final TenantWarningService tenantWarningService;

    @Scheduled(cron = "${cron.process.warnings}")
    public void accountWarningsForDocumentDeletion() {
        LocalDateTime localDateTime = LocalDateTime.now().minusMonths(monthsForDeletionOfDocuments);
        archiveAccounts(localDateTime);
        sendWarningMails(localDateTime);
    }

    private void archiveAccounts(LocalDateTime limitDate) {
        LoggingContext.startTask(TENANT_ARCHIVING);
        processAllWarnings(limitDate, 2);
        archiveCotenantAccounts();
        LoggingContext.endTask();
    }

    private void sendWarningMails(LocalDateTime limitDate) {
        LoggingContext.startTask(TENANT_WARNINGS);
        processAllWarnings(limitDate, 1);
        processAllWarnings(limitDate, 0);
        LoggingContext.endTask();
    }

    private void processAllWarnings(LocalDateTime localDateTime, int warnings) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "id");
        Page<Tenant> tenantPage = tenantRepository.findByLastLoginDateIsBeforeAndHasDocuments(pageable, localDateTime, warnings);
        switch (warnings) {
            case 0 -> log.info("Found {} tenants who will be warned for FIRST time by email", tenantPage.getTotalElements());
            case 1 -> log.info("Found {} tenants who will be warned for SECOND time by email", tenantPage.getTotalElements());
            case 2 -> log.info("Found {} tenants whose documents will be deleted", tenantPage.getTotalElements());
        }

        processWarningsForPage(warnings, tenantPage);

        while (tenantPage.hasNext()) {
            tenantPage = tenantRepository.findByLastLoginDateIsBeforeAndHasDocuments(tenantPage.nextPageable(), localDateTime, warnings);
            processWarningsForPage(warnings, tenantPage);
        }
    }

    private void processWarningsForPage(int warnings, Page<Tenant> tenantPage) {
        tenantPage.stream()
                .filter(tenant -> isNotBlank(tenant.getEmail()))
                .forEach(t -> tryHandlingTenantWarning(warnings, t));
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

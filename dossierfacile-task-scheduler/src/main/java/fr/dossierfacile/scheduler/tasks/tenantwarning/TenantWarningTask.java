package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.scheduler.LoggingContext;
import io.sentry.Sentry;
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

import static fr.dossierfacile.scheduler.tasks.TaskName.TENANT_WARNINGS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantWarningTask {

    @Value("${months_for_deletion_of_documents:3}")
    private Integer monthsForDeletionOfDocuments;
    private final TenantCommonRepository tenantRepository;
    private final TenantWarningService tenantWarningService;

    @Scheduled(cron = "${cron.process.warnings}")
    public void accountWarningsForDocumentDeletion() {
        LoggingContext.startTask(TENANT_WARNINGS);
        LocalDateTime localDateTime = LocalDateTime.now().minusMonths(monthsForDeletionOfDocuments);
        processAllWarnings(localDateTime, 2);
        processAllWarnings(localDateTime, 1);
        processAllWarnings(localDateTime, 0);
        LoggingContext.endTask();
    }

    private void processAllWarnings(LocalDateTime localDateTime, int warnings) {
        long numberOfTenantsToProcess = tenantRepository.countByLastLoginDateIsBeforeAndHasDocuments(localDateTime, warnings);
        if (numberOfTenantsToProcess == 0) {
            return;
        }
        int lengthOfPage = 100;
        int numberOfPage = (int) (numberOfTenantsToProcess / lengthOfPage);
        for (int i = numberOfPage; i >= 0; i--) {
            Pageable page = PageRequest.of(i, lengthOfPage, Sort.Direction.DESC, "id");
            processWarningsBatch(localDateTime, warnings, page);
        }
    }

    private void processWarningsBatch(LocalDateTime localDateTime, int warnings, Pageable page) {
        Page<Tenant> tenantList = tenantRepository.findByLastLoginDateIsBeforeAndHasDocuments(page, localDateTime, warnings);

        switch (warnings) {
            case 0 ->
                    log.info("accountWarnings. Found [" + tenantList.getTotalElements() + "] tenants who will be warned for FIRST time by email");
            case 1 ->
                    log.info("accountWarnings. Found [" + tenantList.getTotalElements() + "] tenants who will be warned for SECOND time by email");
            case 2 ->
                    log.info("accountWarnings. Found [" + tenantList.getTotalElements() + "] tenants whose documents will be deleted");
        }

        tenantList.stream()
                .filter(tenant -> isNotBlank(tenant.getEmail()))
                .forEach(t -> {
            try {
                tenantWarningService.handleTenantWarning(t, warnings);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                Sentry.captureException(e);
            }
        });
    }


}

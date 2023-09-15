package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.TenantWarningService;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledWarningService {
    @Value("${months_for_deletion_of_documents:3}")
    private Integer monthsForDeletionOfDocuments;
    @Value("${months_for_deletion_of_archived_tenants:12}")
    private Integer monthsForDeletionOfTenants;
    @Value("${warnings.max.pages:1}")
    private Integer warningMaxPages;
    private final TenantCommonRepository tenantRepository;
    private final TenantWarningService tenantWarningService;

    @Scheduled(cron = "${cron.process.warnings}")
    public void accountWarningsForDocumentDeletion() {
        log.info("accountWarnings. Executing scheduled task for account warnings at [" + LocalDateTime.now() + "]");
        LocalDateTime localDateTime = LocalDateTime.now().minusMonths(monthsForDeletionOfDocuments);
        deleteOldArchivedAccount();
        processAllWarnings(localDateTime, 2);
        processAllWarnings(localDateTime, 1);
        processAllWarnings(localDateTime, 0);
        log.info("accountWarnings. Account warnings' task was finished");
    }

    private void deleteOldArchivedAccount() {
        LocalDateTime limitDate = LocalDateTime.now().minusMonths(monthsForDeletionOfTenants);
        PageRequest pageRequest = PageRequest.of(0, 5000);
        List<Long> tenantIdList = tenantRepository.findByStatusAndLastUpdateDate(TenantFileStatus.ARCHIVED, limitDate, pageRequest);
        log.info("Delete archived tenants");
        tenantIdList.forEach(tid -> {
            try {
                tenantWarningService.deleteOldArchivedWarning(tid);
            } catch (Exception e) {
                log.error("error while deleting old accounts", e);
            }

        });
        log.info("Deleted " + tenantIdList.size() + " archived tenants");
    }

    private void processAllWarnings(LocalDateTime localDateTime, int warnings) {
        long numberOfTenantsToProcess = tenantRepository.countByLastLoginDateIsBeforeAndHasDocuments(localDateTime, warnings);
        if (numberOfTenantsToProcess == 0) {
            return;
        }
        int lengthOfPage = 100;
        int numberOfPage = (int) (numberOfTenantsToProcess / lengthOfPage);
        if (numberOfPage > warningMaxPages) {
            numberOfPage = warningMaxPages;
        }
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

        tenantList.stream().forEach(t -> {
            try {
                tenantWarningService.handleTenantWarning(t, warnings);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                Sentry.captureException(e);
            }
        });
    }


}

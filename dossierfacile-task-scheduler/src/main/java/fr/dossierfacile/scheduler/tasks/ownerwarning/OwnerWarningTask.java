package fr.dossierfacile.scheduler.tasks.ownerwarning;

import fr.dossierfacile.common.entity.Owner;
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

import static fr.dossierfacile.scheduler.tasks.TaskName.OWNER_DELETE;
import static fr.dossierfacile.scheduler.tasks.TaskName.OWNER_WARNINGS_1;
import static fr.dossierfacile.scheduler.tasks.TaskName.OWNER_WARNINGS_2;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerWarningTask {
    private static final int PAGE_SIZE = 100;

    @Value("${owner_weeks_for_deletion:104}")
    private Integer ownerWeeksForDeletion;
    @Value("${owner_weeks_for_first_warning_deletion:100}")
    private Integer ownerWeeksForFirstWarningDelete;
    @Value("${owner_weeks_for_second_warning_deletion:103}")
    private Integer ownerWeeksForSecondWarningDelete;
    private final OwnerRepository ownerRepository;
    private final OwnerWarningService ownerWarningService;

    @Scheduled(cron = "${cron.owner.delete:10 10 * * * 5}")
    public void accountWarningsForDocumentDeletion() {
        // Delete 2 years old Accounts
        LocalDateTime limitDate = LocalDateTime.now().minusWeeks(ownerWeeksForDeletion);
        LoggingContext.startTask(OWNER_DELETE);
        processAllWarnings(limitDate, 2);
        LoggingContext.endTask();

        // Send second warning
        limitDate = LocalDateTime.now().minusWeeks(ownerWeeksForSecondWarningDelete);
        LoggingContext.startTask(OWNER_WARNINGS_2);
        processAllWarnings(limitDate, 1);
        LoggingContext.endTask();

        // Send First warning
        limitDate = LocalDateTime.now().minusWeeks(ownerWeeksForFirstWarningDelete);
        LoggingContext.startTask(OWNER_WARNINGS_1);
        processAllWarnings(limitDate, 0);
        LoggingContext.endTask();

    }

    private void processAllWarnings(LocalDateTime localDateTime, int warnings) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "id");
        Page<Owner> ownerPage = ownerRepository.findByLastLoginDate(pageable, localDateTime, warnings);
        switch (warnings) {
            case 0 ->
                    log.info("Found {} owners who will be warned for FIRST time by email", ownerPage.getTotalElements());
            case 1 ->
                    log.info("Found {} owners who will be warned for SECOND time by email", ownerPage.getTotalElements());
            case 2 -> log.info("Found {} owners whose account will be deleted", ownerPage.getTotalElements());
        }

        processWarningsForPage(warnings, ownerPage);

        while (ownerPage.hasNext()) {
            ownerPage = ownerRepository.findByLastLoginDate(ownerPage.nextPageable(), localDateTime, warnings);
            processWarningsForPage(warnings, ownerPage);
        }
    }

    private void processWarningsForPage(int warnings, Page<Owner> ownerPage) {
        ownerPage.stream()
                .filter(owner -> isNotBlank(owner.getEmail()))
                .forEach(o -> {
                    try {
                        ownerWarningService.handleOwnerWarning(o, warnings);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
    }
}

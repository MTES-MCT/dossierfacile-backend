package fr.dossierfacile.scheduler.tasks.ownerwarning;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.logging.util.LoggerUtil;
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
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static fr.dossierfacile.scheduler.tasks.TaskName.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerWarningTask extends AbstractTask {
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
        super.startTask(OWNER_DELETE);
        processAllWarnings(limitDate, 2);
        super.endTask();

        // Send second warning
        limitDate = LocalDateTime.now().minusWeeks(ownerWeeksForSecondWarningDelete);
        super.startTask(OWNER_WARNINGS_2);
        processAllWarnings(limitDate, 1);
        super.endTask();

        // Send First warning
        limitDate = LocalDateTime.now().minusWeeks(ownerWeeksForFirstWarningDelete);
        super.startTask(OWNER_WARNINGS_1);
        processAllWarnings(limitDate, 0);
        super.endTask();

    }

    private void processAllWarnings(LocalDateTime localDateTime, int warnings) {
        List<Long> listOfOwnerIds = List.of();
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "id");
        Page<Owner> ownerPage = ownerRepository.findByLastLoginDate(pageable, localDateTime, warnings);
        switch (warnings) {
            case 0 ->
                    log.info("Found {} owners who will be warned for FIRST time by email", ownerPage.getTotalElements());
            case 1 ->
                    log.info("Found {} owners who will be warned for SECOND time by email", ownerPage.getTotalElements());
            case 2 -> log.info("Found {} owners whose account will be deleted", ownerPage.getTotalElements());
        }

        processWarningsForPage(warnings, ownerPage, listOfOwnerIds);

        while (ownerPage.hasNext()) {
            ownerPage = ownerRepository.findByLastLoginDate(ownerPage.nextPageable(), localDateTime, warnings);
            processWarningsForPage(warnings, ownerPage, listOfOwnerIds);
        }
        addOwnerIdsToForLogging(listOfOwnerIds);
    }

    private void processWarningsForPage(int warnings, Page<Owner> ownerPage, List<Long> listOfOwnerIds) {
        ownerPage.stream()
                .filter(owner -> isNotBlank(owner.getEmail()))
                .forEach(o -> {
                    try {
                        listOfOwnerIds.add(o.getId());
                        ownerWarningService.handleOwnerWarning(o, warnings);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
    }

    private void addOwnerIdsToForLogging(List<Long> ownerIds) {
        if (!CollectionUtils.isEmpty(ownerIds)) {
            var ids = ownerIds.stream().map(String::valueOf).collect(Collectors.joining(" , "));
            LoggerUtil.addTaskOwnerList("[" + ids + "]");
        }
    }
}

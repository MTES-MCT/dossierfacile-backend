package fr.dossierfacile.scheduler.tasks.completedoptin;

import fr.dossierfacile.scheduler.tasks.AbstractTask;
import fr.dossierfacile.scheduler.tasks.TaskName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily reminder to the tenants whose dossier is COMPLETED and who did not ask for an
 * operator verification (see docs/completed-optin.md). A tenant is reminded on the first
 * run following the 24 hours after the submission, and only once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationReminderTask extends AbstractTask {

    private final CompletedOptinTenantRepository tenantRepository;
    private final ValidationReminderService validationReminderService;

    @Value("${tenant.validation.reminder.min-age-hours:24}")
    private long minAgeHours;
    // Upper bound: no reminder for the dossiers completed long before the task was enabled,
    // while leaving room to catch up a missed run
    @Value("${tenant.validation.reminder.max-age-hours:72}")
    private long maxAgeHours;

    @Scheduled(cron = "${cron.validation.reminder:0 0 10 * * *}", zone = "Europe/Paris")
    public void sendValidationReminders() {
        super.startTask(TaskName.TENANT_VALIDATION_REMINDER);
        try {
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            List<Long> tenantIds = tenantRepository.findTenantIdsToRemindForValidation(
                    now.minusHours(maxAgeHours), now.minusHours(minAgeHours));
            log.info("Found {} tenants to remind about the operator verification", tenantIds.size());

            List<Long> remindedTenantIds = new ArrayList<>();
            for (Long tenantId : tenantIds) {
                try {
                    if (validationReminderService.sendReminder(tenantId)) {
                        remindedTenantIds.add(tenantId);
                    }
                } catch (Exception e) {
                    log.error("Error while sending the validation reminder to tenant [{}]: {}", tenantId, e.getMessage(), e);
                }
            }
            countTenantIdForLogging(remindedTenantIds);
        } catch (Exception e) {
            log.error("Error during the validation reminder task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }
}

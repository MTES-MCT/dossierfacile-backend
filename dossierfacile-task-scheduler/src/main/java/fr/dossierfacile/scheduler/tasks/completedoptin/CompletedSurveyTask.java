package fr.dossierfacile.scheduler.tasks.completedoptin;

import fr.dossierfacile.scheduler.tasks.AbstractTask;
import fr.dossierfacile.scheduler.tasks.TaskName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily impact survey to the tenants whose dossier is still COMPLETED 6 weeks after the
 * submission (see docs/completed-optin.md). A tenant receives it on the first run following
 * the 42 days after the last submission, and only once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompletedSurveyTask extends AbstractTask {

    private final CompletedOptinTenantRepository tenantRepository;
    private final CompletedSurveyService completedSurveyService;

    @Value("${tenant.completed.survey.min-age-days:42}")
    private long minAgeDays;
    // Upper bound: no survey for the dossiers completed long before the task was enabled,
    // while leaving room to catch up missed runs
    @Value("${tenant.completed.survey.max-age-days:45}")
    private long maxAgeDays;

    @Scheduled(cron = "${cron.completed.survey:0 30 10 * * *}", zone = "Europe/Paris")
    public void sendCompletedSurveys() {
        super.startTask(TaskName.TENANT_COMPLETED_SURVEY);
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Long> tenantIds = tenantRepository.findTenantIdsForCompletedSurvey(
                    now.minusDays(maxAgeDays), now.minusDays(minAgeDays));
            log.info("Found {} tenants to send the completed dossier survey to", tenantIds.size());

            List<Long> surveyedTenantIds = new ArrayList<>();
            for (Long tenantId : tenantIds) {
                try {
                    if (completedSurveyService.sendSurvey(tenantId)) {
                        surveyedTenantIds.add(tenantId);
                    }
                } catch (Exception e) {
                    log.error("Error while sending the completed dossier survey to tenant [{}]: {}", tenantId, e.getMessage(), e);
                }
            }
            countTenantIdForLogging(surveyedTenantIds);
        } catch (Exception e) {
            log.error("Error during the completed dossier survey task: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }
}

package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.repository.ConfirmationTokenRepository;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.ScheduledTasksService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasksServiceImpl implements ScheduledTasksService {
    private final TenantRepository tenantRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final MailService mailService;
    @Value("${days_for_email_account_validation_reminder}")
    private Long daysForEmailAccountValidationReminder;
    @Value("${days_for_account_completion_reminder}")
    private Long daysForAccountCompletionReminder;
    @Value("${days_for_account_declination_reminder}")
    private Long daysForAccountDeclinationReminder;
    @Value("${days_for_satisfaction_email}")
    private Long daysForSatisfactionEmail;

    /**
     * Email notifications, if needed, will begin at 12:10 am every day
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void sendRemindingEmailNotifications() {
        emailAccountValidationReminder();
        accountCompletionReminder();
        accountDeclinationReminder();
        satisfactionEmails();
    }

    @Override
    public void emailAccountValidationReminder() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusDays(daysForEmailAccountValidationReminder)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);
        LocalDateTime endDate = LocalDateTime.now()
                .minusDays(daysForEmailAccountValidationReminder)
                .withHour(23)
                .withMinute(59)
                .withSecond(59);
        List<Tenant> tenantsToNotificate = tenantRepository.findAllByEnabledIsFalseAndCreationDateTimeIsBetween(startDate, endDate);
        if (tenantsToNotificate != null && tenantsToNotificate.size() > 0) {
            log.info(tenantsToNotificate.size() + " tenants found, to be notified because account email not yet validated after " + daysForEmailAccountValidationReminder + " days of account registration");
            for (Tenant tenant : tenantsToNotificate) {
                confirmationTokenRepository.findByUser(tenant)
                        .ifPresent(confirmationToken -> mailService.sendEmailWhenEmailAccountNotYetValidated(tenant, confirmationToken));
            }
        }
    }

    @Override
    public void accountCompletionReminder() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusDays(daysForAccountCompletionReminder)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);
        LocalDateTime endDate = LocalDateTime.now()
                .minusDays(daysForAccountCompletionReminder)
                .withHour(23)
                .withMinute(59)
                .withSecond(59);
        List<Tenant> tenantsToNotificate = tenantRepository.findAllByHonorDeclarationIsFalseAndCompletionDateTimeIsBetween(startDate, endDate);
        if (tenantsToNotificate != null && tenantsToNotificate.size() > 0) {
            log.info(tenantsToNotificate.size() + " tenants found, to be notified because account not yet completed after " + daysForAccountCompletionReminder + " days of account email validation");
            for (Tenant tenant : tenantsToNotificate) {
                mailService.sendEmailWhenAccountNotYetCompleted(tenant);
            }
        }
    }

    @Override
    public void accountDeclinationReminder() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusDays(daysForAccountDeclinationReminder)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);
        LocalDateTime endDate = LocalDateTime.now()
                .minusDays(daysForAccountDeclinationReminder)
                .withHour(23)
                .withMinute(59)
                .withSecond(59);
        List<Tenant> tenantsToNotificate = tenantRepository.findAllDeclinedSinceXDaysAgo(startDate, endDate);
        if (tenantsToNotificate != null && tenantsToNotificate.size() > 0) {
            log.info(tenantsToNotificate.size() + " tenants found, to be notified because account is still declination after " + daysForAccountCompletionReminder + " days of account declined");
            for (Tenant tenant : tenantsToNotificate) {
                mailService.sendEmailWhenAccountIsStillDeclined(tenant);
            }
        }
    }

    @Override
    public void satisfactionEmails() {
        LocalDateTime startDate = LocalDateTime.now()
                .minusDays(daysForSatisfactionEmail)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);
        LocalDateTime endDate = LocalDateTime.now()
                .minusDays(daysForSatisfactionEmail)
                .withHour(23)
                .withMinute(59)
                .withSecond(59);
        //region Notification to Tenants NOT associated to any partner
        List<Tenant> tenantsToNotificate1 = tenantRepository.findAllTenantsNotAssociatedToPartnersAndValidatedSinceXDaysAgo(startDate, endDate);
        if (tenantsToNotificate1 != null && tenantsToNotificate1.size() > 0) {
            log.info(tenantsToNotificate1.size() + " tenants NOT associated to partners and validated since  " + daysForSatisfactionEmail + " days ago");
            for (Tenant tenant : tenantsToNotificate1) {
                mailService.sendEmailWhenTenantNOTAssociatedToPartnersAndValidatedXDaysAgo(tenant);
            }
        }
        //endregion
        //region Notification to Tenants associated to partners
        List<Tenant> tenantsToNotificate2 = tenantRepository.findAllTenantsYESAssociatedToPartnersAndValidatedSinceXDaysAgo(startDate, endDate);
        if (tenantsToNotificate2 != null && tenantsToNotificate2.size() > 0) {
            log.info(tenantsToNotificate2.size() + " tenants associated to partners and validated since " + daysForSatisfactionEmail + " days ago");
            for (Tenant tenant : tenantsToNotificate2) {
                mailService.sendEmailWhenTenantYESAssociatedToPartnersAndValidatedXDaysAgo(tenant);
            }
        }
        //endregion
    }
}

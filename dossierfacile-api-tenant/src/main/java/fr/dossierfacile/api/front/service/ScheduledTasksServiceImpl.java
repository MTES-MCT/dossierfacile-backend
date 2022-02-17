package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.ConfirmationTokenNotFoundException;
import fr.dossierfacile.api.front.repository.ConfirmationTokenRepository;
import fr.dossierfacile.api.front.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.LogService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.ScheduledTasksService;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
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
public class ScheduledTasksServiceImpl implements ScheduledTasksService {
    private final TenantCommonRepository tenantRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final MailService mailService;
    private final LogService logService;
    private final ConfirmationTokenService confirmationTokenService;
    private final DocumentService documentService;
    @Value("${days_for_email_account_validation_reminder}")
    private Long daysForEmailAccountValidationReminder;
    @Value("${days_for_account_completion_reminder}")
    private Long daysForAccountCompletionReminder;
    @Value("${days_for_account_declination_reminder}")
    private Long daysForAccountDeclinationReminder;
    @Value("${days_for_satisfaction_email}")
    private Long daysForSatisfactionEmail;
    @Value("${months_for_deletion_of_documents}")
    private Integer monthsForDeletionOfDocuments;

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

    @Scheduled(cron = "0 0 17 * * 5")
    public void accountWarningsForDocumentDeletion() {
        log.info("Executing scheduled task for account warnings at [" + LocalDateTime.now() + "]");
        LocalDateTime localDateTime = LocalDateTime.now()
                .minusMonths(monthsForDeletionOfDocuments);

        int lengthOfPage = 100;
        Pageable page = PageRequest.of(0, lengthOfPage, Sort.Direction.DESC, "id");
        Page<Tenant> tenantList = tenantRepository.findByLastLoginDateIsBeforeAndHasDocuments(page, localDateTime);
        log.info("Found [" + tenantList.getTotalElements() + "] tenants that needs to be warned because they have not logged in for [" + monthsForDeletionOfDocuments + "] months");

        if (tenantList.getTotalElements() > 0) {
            while (!tenantList.isEmpty()) {
                page = page.next();

                long firstWarning = tenantList.stream().filter(tenant -> tenant.getWarnings() == 0).count();
                log.info("- [" + firstWarning + "] of them to be warned with first email warning");
                long secondWarning = tenantList.stream().filter(tenant -> tenant.getWarnings() == 1).count();
                log.info("- [" + secondWarning + "] of them to be warned with second email warning");
                long withDocumentsToDelete = tenantList.stream().filter(tenant -> tenant.getWarnings() == 2).count();
                log.info("- To [" + withDocumentsToDelete + "] of them will have their documents deleted");

                for (Tenant t : tenantList) {
                    switch (t.getWarnings()) {
                        case 0 -> {
                            t.setWarnings(1);
                            tenantRepository.save(t);
                            mailService.sendEmailFirstWarningForDeletionOfDocuments(t, confirmationTokenService.createToken(t));
                            logService.saveLog(LogType.FIRST_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, t.getId());
                        }
                        case 1 -> {
                            t.setWarnings(2);
                            tenantRepository.save(t);
                            mailService.sendEmailSecondWarningForDeletionOfDocuments(t, confirmationTokenService.createToken(t));
                            logService.saveLog(LogType.SECOND_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, t.getId());
                        }
                        case 2 -> {
                            t.setWarnings(0);
                            t.setConfirmationToken(null);
                            t.setHonorDeclaration(false);
                            t.setStatus(TenantFileStatus.INCOMPLETE);
                            tenantRepository.save(t);

                            ConfirmationToken confirmationToken = confirmationTokenRepository.findByUser(t).orElseThrow(() -> new ConfirmationTokenNotFoundException(t.getId()));
                            confirmationTokenRepository.delete(confirmationToken);

                            documentService.deleteAllDocumentsAssociatedToTenant(t);
                            logService.saveLog(LogType.DOCUMENT_DELETION_AFTER_2_ACCOUNT_WARNINGS, t.getId());
                        }
                    }
                }
                tenantList = tenantRepository.findByLastLoginDateIsBeforeAndHasDocuments(page, localDateTime);
            }
        }
        log.info("Account warnings' task finished");
    }
}

package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.ConfirmationTokenNotFoundException;
import fr.dossierfacile.api.front.repository.ConfirmationTokenRepository;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.GuarantorService;
import fr.dossierfacile.api.front.service.interfaces.LogService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.ScheduledTasksService;
import fr.dossierfacile.common.entity.ConfirmationToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
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
    private final DocumentRepository documentRepository;
    private final GuarantorRepository guarantorRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final MailService mailService;
    private final LogService logService;
    private final ConfirmationTokenService confirmationTokenService;
    private final DocumentService documentService;
    private final GuarantorService guarantorService;
    private final PartnerCallBackService partnerCallBackService;
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

    @Scheduled(cron = "0 0 8 * * 5")
    public void accountWarningsForDocumentDeletion() {
        log.info("accountWarnings. Executing scheduled task for account warnings at [" + LocalDateTime.now() + "]");

        LocalDateTime localDateTime = LocalDateTime.now().minusMonths(monthsForDeletionOfDocuments);

        processWarnings(2, localDateTime);
        processWarnings(1, localDateTime);
        processWarnings(0, localDateTime);

        log.info("accountWarnings. Account warnings' task was finished");
    }

    public void processWarnings(int warnings, LocalDateTime localDateTime) {
        int lengthOfPage = 100;
        Pageable page = PageRequest.of(0, lengthOfPage, Sort.Direction.DESC, "id");
        Page<Tenant> tenantList = tenantRepository.findByLastLoginDateIsBeforeAndHasDocuments(page, localDateTime, warnings);

        switch (warnings) {
            case 0 -> log.info("accountWarnings. Found [" + tenantList.getTotalElements() + "] tenants who will be warned for FIRST time by email");
            case 1 -> log.info("accountWarnings. Found [" + tenantList.getTotalElements() + "] tenants who will be warned for SECOND time by email");
            case 2 -> log.info("accountWarnings. Found [" + tenantList.getTotalElements() + "] tenants whose documents will be deleted");
        }

        if (tenantList.getTotalElements() > 0) {
            while (!tenantList.isEmpty()) {
                page = page.next();

                for (Tenant t : tenantList) {
                    switch (warnings) {
                        case 0 -> {
                            log.info("accountWarnings. FIRST warning for tenant with ID [" + t.getId() + "]");
                            t.setWarnings(1);
                            tenantRepository.save(t);
                            mailService.sendEmailFirstWarningForDeletionOfDocuments(t, confirmationTokenService.createToken(t));
                            logService.saveLog(LogType.FIRST_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, t.getId());
                        }
                        case 1 -> {
                            log.info("accountWarnings. SECOND warning for tenant with ID [" + t.getId() + "]");
                            t.setWarnings(2);
                            tenantRepository.save(t);
                            mailService.sendEmailSecondWarningForDeletionOfDocuments(t, confirmationTokenService.createToken(t));
                            logService.saveLog(LogType.SECOND_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION, t.getId());
                        }
                        case 2 -> {
                            log.info("accountWarnings. Documents deletion for tenant with ID [" + t.getId() + "]");
                            t.setWarnings(0);
                            t.setConfirmationToken(null);
                            t.setHonorDeclaration(false);
                            t.setStatus(TenantFileStatus.ARCHIVED);
                            t.setZipCode("");
                            t.setClarification("");
                            tenantRepository.save(t);

                            ConfirmationToken confirmationToken = confirmationTokenRepository.findByUser(t).orElseThrow(() -> new ConfirmationTokenNotFoundException(t.getId()));
                            confirmationTokenRepository.delete(confirmationToken);

                            documentService.deleteAllDocumentsAssociatedToTenant(t);
                            guarantorService.deleteAllGuaratorsAssociatedToTenant(t);
                            logService.saveLog(LogType.DOCUMENT_DELETION_AFTER_2_ACCOUNT_WARNINGS, t.getId());
                            partnerCallBackService.sendCallBack(t, PartnerCallBackType.ARCHIVED_ACCOUNT);
                        }
                    }
                }

                tenantList = tenantRepository.findByLastLoginDateIsBeforeAndHasDocuments(page, localDateTime, warnings);
            }

            log.info("accountWarnings. Flushing all elements related to " + (warnings == 0 ? "FIRST email warning" : warnings == 1 ? "SECOND email warning" : "deletion of documents"));
            switch (warnings) {
                case 0 -> tenantRepository.flush();
                case 1 -> tenantRepository.flush();
                case 2 -> {
                    tenantRepository.flush();
                    confirmationTokenRepository.flush();
                    documentRepository.flush();
                    guarantorRepository.flush();
                }
            }
            log.info("accountWarnings. END. Flushing all elements related to " + (warnings == 0 ? "FIRST email warning" : warnings == 1 ? "SECOND email warning" : "deletion of documents"));
        }
    }
}

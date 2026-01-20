package fr.dossierfacile.scheduler.tasks.ownerwarning;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.enums.OwnerLogType;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.KeycloakCommonService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.utils.TransactionalUtil;
import fr.dossierfacile.scheduler.tasks.tenantwarning.WarningMailSender;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class OwnerWarningService {
    private final LogService logService;
    private final WarningMailSender mailSender;
    private final ConfirmationTokenService confirmationTokenService;
    private final OwnerRepository ownerRepository;
    private final KeycloakCommonService keycloakCommonService;

    /**
     * Handles owner warnings (0, 1) in a transaction.
     * For deletion (warning 2), see {@link #deleteOwnerAccount(Owner)}.
     */
    @Transactional
    public void handleOwnerWarning(Owner o, int warnings) {
        Optional<Owner> optionalOwner = ownerRepository.findById(o.getId());
        if (optionalOwner.isEmpty()) {
            return;
        }
        Owner owner = optionalOwner.get();
        switch (warnings) {
            case 0 -> handleFirstWarning(owner);
            case 1 -> handleSecondWarning(owner);
            case 2 -> deleteOwnerAccount(owner);
        }
    }

    /**
     * Deletes owner account: first performs all database/keycloak operations,
     * then schedules the confirmation email to be sent AFTER transaction commit.
     * This ensures the email is only sent if the account deletion transaction commits successfully.
     * If the transaction rolls back, the email will not be sent, preventing duplicate emails on retry.
     */
    private void deleteOwnerAccount(Owner owner) {
        Long ownerId = owner.getId();
        log.info("Deleting owner {}", ownerId);

        // 1. Log the deletion first (while we still have the owner data)
        logService.saveLogWithOwnerData(OwnerLogType.ACCOUNT_DELETED, owner);

        // 2. Delete from Keycloak
        keycloakCommonService.deleteKeycloakUser(owner);

        // 3. Delete from database
        ownerRepository.delete(owner);
        ownerRepository.flush(); // Force the delete to execute now

        log.info("Owner {} successfully deleted from database and Keycloak", ownerId);

        // 4. Schedule email to be sent AFTER transaction commit
        // This ensures the email is only sent if the transaction commits successfully.
        // If the transaction rolls back, the email will not be sent, preventing duplicate emails.
        // Note: We can use 'owner' directly here because the object still exists in memory
        // with its field values, even though it's been removed from the persistence context.
        TransactionalUtil.afterCommit(() -> {
            mailSender.sendEmailOwnerDeleted(owner);
        });
    }

    private void handleSecondWarning(Owner o) {
        mailSender.sendEmailSecondWarningForDeletionOfOwner(o, confirmationTokenService.createToken(o));
        o.setWarnings(2);
        ownerRepository.save(o);
        logService.saveLogWithOwnerData(OwnerLogType.SECOND_ACCOUNT_WARNING_FOR_DELETION, o);
    }

    private void handleFirstWarning(Owner o) {
        mailSender.sendEmailFirstWarningForDeletionOfOwner(o, confirmationTokenService.createToken(o));
        o.setWarnings(1);
        ownerRepository.save(o);
        logService.saveLogWithOwnerData(OwnerLogType.FIRST_ACCOUNT_WARNING_FOR_DELETION, o);
    }

}

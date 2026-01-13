package fr.dossierfacile.scheduler.tasks.ownerwarning;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.enums.OwnerLogType;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.KeycloakCommonService;
import fr.dossierfacile.common.service.interfaces.LogService;
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

    @Transactional
    public void handleOwnerWarning(Owner o, int warnings) {
        Optional<Owner> optionalOwner = ownerRepository.findById(o.getId());
        if (optionalOwner.isEmpty()) {
            return;
        }
        Owner owner = optionalOwner.get();
        switch (warnings) {
            case 0 -> handleWarning0(owner);
            case 1 -> handleWarning1(owner);
            case 2 -> handleWarning2(owner);
        }
    }

    private void handleWarning2(Owner owner) {
        log.info("Deleting owner {}", owner.getId());
        mailSender.sendEmailOwnerDeleted(owner);
        logService.saveLogWithOwnerData(OwnerLogType.ACCOUNT_DELETED, owner);
        keycloakCommonService.deleteKeycloakUser(owner);
        ownerRepository.delete(owner);
    }

    private void handleWarning1(Owner o) {
        mailSender.sendEmailSecondWarningForDeletionOfOwner(o, confirmationTokenService.createToken(o));
        o.setWarnings(2);
        ownerRepository.save(o);
        logService.saveLogWithOwnerData(OwnerLogType.SECOND_ACCOUNT_WARNING_FOR_DELETION, o);
    }

    private void handleWarning0(Owner o) {
        mailSender.sendEmailFirstWarningForDeletionOfOwner(o, confirmationTokenService.createToken(o));
        o.setWarnings(1);
        ownerRepository.save(o);
        logService.saveLogWithOwnerData(OwnerLogType.FIRST_ACCOUNT_WARNING_FOR_DELETION, o);
    }

}

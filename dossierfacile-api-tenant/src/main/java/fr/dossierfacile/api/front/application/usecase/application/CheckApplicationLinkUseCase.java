package fr.dossierfacile.api.front.application.usecase.application;

import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.common.application.usecase.BaseUseCase;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

/**
 * HEAD /full/{token}: checks that a full-data link exists and is not blocked
 */
@Component
public class CheckApplicationLinkUseCase
        extends BaseUseCase<CheckApplicationLinkUseCase.CheckApplicationLinkCommand, Void> {

    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final BruteForceProtectionService bruteForceProtectionService;

    public CheckApplicationLinkUseCase(
            PlatformTransactionManager transactionManager,
            ApartmentSharingLinkRepository apartmentSharingLinkRepository,
            BruteForceProtectionService bruteForceProtectionService
    ) {
        super(transactionManager);
        this.apartmentSharingLinkRepository = apartmentSharingLinkRepository;
        this.bruteForceProtectionService = bruteForceProtectionService;
    }

    @Override
    public Void execute(CheckApplicationLinkCommand command) {
        checkTransaction();

        ApartmentSharingLink link = apartmentSharingLinkRepository.findValidLinkByToken(command.token(), true)
                .orElseThrow(() -> new ApartmentSharingNotFoundException(command.token().toString()));

        bruteForceProtectionService.checkAndEnforceProtection(link);
        return null;
    }

    public record CheckApplicationLinkCommand(UUID token) {
    }
}

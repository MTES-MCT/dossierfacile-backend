package fr.dossierfacile.api.front.application.usecase.application;

import fr.dossierfacile.api.front.application.projection.ApplicationProjectionLoader;
import fr.dossierfacile.api.front.application.projection.FullApplicationResponseProjection;
import fr.dossierfacile.api.front.application.projection.ApplicationProjectionSources;
import fr.dossierfacile.api.front.domain.policy.TrigramAccessPolicy;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.TrigramNotAuthorizedException;
import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.common.application.usecase.BaseUseCase;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class GetFullApplicationUseCase
        extends BaseUseCase<GetFullApplicationUseCase.GetFullApplicationCommand, ApplicationModel> {

    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final TrigramAccessPolicy trigramAccessPolicy;
    private final ApplicationProjectionLoader applicationProjectionLoader;
    private final FullApplicationResponseProjection fullApplicationResponseProjection;
    private final LinkLogService linkLogService;

    public GetFullApplicationUseCase(
            PlatformTransactionManager transactionManager,
            ApartmentSharingLinkRepository apartmentSharingLinkRepository,
            BruteForceProtectionService bruteForceProtectionService,
            TrigramAccessPolicy trigramAccessPolicy,
            ApplicationProjectionLoader applicationProjectionLoader,
            FullApplicationResponseProjection fullApplicationResponseProjection,
            LinkLogService linkLogService
    ) {
        super(transactionManager);
        this.apartmentSharingLinkRepository = apartmentSharingLinkRepository;
        this.bruteForceProtectionService = bruteForceProtectionService;
        this.trigramAccessPolicy = trigramAccessPolicy;
        this.applicationProjectionLoader = applicationProjectionLoader;
        this.fullApplicationResponseProjection = fullApplicationResponseProjection;
        this.linkLogService = linkLogService;
    }

    /**
     * The brute-force writes (recordFailedAttempt/resetAttempts) and the link log MUST stay outside
     * the loading transaction: the legacy path commits the failed-attempt counter before propagating
     * the 403. Wrapping the whole flow in one transaction would roll the counter back on every
     * rejection and disable the protection. Only the aggregate loading is transactional.
     */
    @Override
    public ApplicationModel execute(GetFullApplicationCommand command) {
        checkTransaction();

        ApartmentSharingLink link = apartmentSharingLinkRepository.findValidLinkByToken(command.token(), true)
                .orElseThrow(() -> new ApartmentSharingNotFoundException(command.token().toString()));

        bruteForceProtectionService.checkAndEnforceProtection(link);

        // sources are loaded in a single explicit transaction
        ApplicationProjectionSources sources = this.<ApplicationProjectionSources>executeInTransaction(status ->
                        applicationProjectionLoader.load(link.getApartmentSharing().getId()))
                .orElseThrow();

        try {
            trigramAccessPolicy.validateAccess(command.trigram(), sources.tenants());
        } catch (TrigramNotAuthorizedException e) {
            bruteForceProtectionService.recordFailedAttempt(link);
            throw e;
        }
        bruteForceProtectionService.resetAttempts(link);

        // Legacy behavior: the consultation is logged unless the logged-in tenant consults their own application
        if (!Objects.equals(command.loggedTenantApartmentSharingId(), link.getApartmentSharing().getId())) {
            linkLogService.save(new LinkLog(link.getApartmentSharing(), command.token(), LinkType.FULL_APPLICATION, command.ipAddress()));
        }

        return fullApplicationResponseProjection.project(sources, command.token());
    }

    public record GetFullApplicationCommand(UUID token, String trigram,
                                            Long loggedTenantApartmentSharingId, String ipAddress) {
    }
}

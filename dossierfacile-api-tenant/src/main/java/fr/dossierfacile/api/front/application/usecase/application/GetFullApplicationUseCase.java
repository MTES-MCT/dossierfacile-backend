package fr.dossierfacile.api.front.application.usecase.application;

import fr.dossierfacile.api.front.application.projection.ApplicationProjection;
import fr.dossierfacile.api.front.application.projection.ApplicationProjectionLoader;
import fr.dossierfacile.api.front.application.projection.ApplicationProjectionSources;
import fr.dossierfacile.api.front.application.projection.ApplicationReadView;
import fr.dossierfacile.api.front.application.projection.FullApplicationViewAssembler;
import fr.dossierfacile.api.front.domain.policy.LinkBruteForcePolicy;
import fr.dossierfacile.api.front.domain.policy.TrigramAccessPolicy;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.TrigramNotAuthorizedException;
import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.common.application.usecase.BaseUseCase;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
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
    private final LinkBruteForcePolicy linkBruteForcePolicy;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final TrigramAccessPolicy trigramAccessPolicy;
    private final ApplicationProjectionLoader applicationProjectionLoader;
    private final FullApplicationViewAssembler fullApplicationViewAssembler;
    private final ApplicationProjection applicationProjection;
    private final LinkLogService linkLogService;
    private final JpaTenantRepository jpaTenantRepository;

    public GetFullApplicationUseCase(
            PlatformTransactionManager transactionManager,
            ApartmentSharingLinkRepository apartmentSharingLinkRepository,
            LinkBruteForcePolicy linkBruteForcePolicy,
            BruteForceProtectionService bruteForceProtectionService,
            TrigramAccessPolicy trigramAccessPolicy,
            ApplicationProjectionLoader applicationProjectionLoader,
            FullApplicationViewAssembler fullApplicationViewAssembler,
            ApplicationProjection applicationProjection,
            LinkLogService linkLogService,
            JpaTenantRepository jpaTenantRepository
    ) {
        super(transactionManager);
        this.apartmentSharingLinkRepository = apartmentSharingLinkRepository;
        this.linkBruteForcePolicy = linkBruteForcePolicy;
        this.bruteForceProtectionService = bruteForceProtectionService;
        this.trigramAccessPolicy = trigramAccessPolicy;
        this.applicationProjectionLoader = applicationProjectionLoader;
        this.fullApplicationViewAssembler = fullApplicationViewAssembler;
        this.applicationProjection = applicationProjection;
        this.linkLogService = linkLogService;
        this.jpaTenantRepository = jpaTenantRepository;
    }

    /**
     * Single transaction for the whole flow (the use case is the transaction root).
     * The brute-force writes (recordFailedAttempt/resetAttempts) run in REQUIRES_NEW
     * transactions of their own: the failed-attempt counter must be committed even though
     * the TrigramNotAuthorizedException rolls this transaction back
     */
    @Override
    public ApplicationModel execute(GetFullApplicationCommand command) {
        // check that this method cannot be called from an existing transaction
        checkTransaction();

        return this.<ApplicationModel>executeInTransaction(status -> {
            ApartmentSharingLink link = apartmentSharingLinkRepository.findValidLinkByToken(command.token(), true)
                    .orElseThrow(() -> new ApartmentSharingNotFoundException(command.token().toString()));

            linkBruteForcePolicy.checkNotBlocked(link);

            ApplicationProjectionSources sources = applicationProjectionLoader.load(link.getApartmentSharing().getId());

            try {
                trigramAccessPolicy.validateAccess(command.trigram(), sources.tenants());
            } catch (TrigramNotAuthorizedException e) {
                // Committed in its own REQUIRES_NEW transaction, survives this rollback
                bruteForceProtectionService.recordFailedAttempt(link);
                throw e;
            }
            bruteForceProtectionService.resetAttempts(link);

            // Legacy behavior: the consultation is logged unless the logged-in tenant consults their own application
            if (!isViewerOwnApplication(command.viewerKeycloakId(), link.getApartmentSharing().getId())) {
                linkLogService.save(new LinkLog(link.getApartmentSharing(), command.token(), LinkType.FULL_APPLICATION, command.ipAddress()));
            }

            ApplicationReadView view = fullApplicationViewAssembler.assemble(sources, command.token());
            return applicationProjection.project(view);
        }).orElseThrow();
    }

    private boolean isViewerOwnApplication(String viewerKeycloakId, Long apartmentSharingId) {
        if (viewerKeycloakId == null) {
            log.info("Anonymous request to get full application");
            return false;
        }
        return jpaTenantRepository.findByKeycloakId(viewerKeycloakId)
                .map(viewer -> {
                    log.info("Authenticated request to get full application, tenantId: {}", viewer.getId());
                    return Objects.equals(viewer.getApartmentSharingId(), apartmentSharingId);
                })
                .orElse(false);
    }

    public record GetFullApplicationCommand(UUID token, String trigram,
                                            String viewerKeycloakId, String ipAddress) {
    }
}

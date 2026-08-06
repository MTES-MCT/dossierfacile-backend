package fr.dossierfacile.api.front.application.usecase.application;

import fr.dossierfacile.api.front.application.projection.ApplicationProjectionLoader;
import fr.dossierfacile.api.front.application.projection.ApplicationProjectionSources;
import fr.dossierfacile.api.front.application.projection.LightApplicationResponseProjection;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
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

import java.util.UUID;

@Component
@Slf4j
public class GetLightApplicationUseCase
        extends BaseUseCase<GetLightApplicationUseCase.GetLightApplicationCommand, ApplicationModel> {

    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final ApplicationProjectionLoader applicationProjectionLoader;
    private final LightApplicationResponseProjection lightApplicationResponseProjection;
    private final LinkLogService linkLogService;

    public GetLightApplicationUseCase(
            PlatformTransactionManager transactionManager,
            ApartmentSharingLinkRepository apartmentSharingLinkRepository,
            ApplicationProjectionLoader applicationProjectionLoader,
            LightApplicationResponseProjection lightApplicationResponseProjection,
            LinkLogService linkLogService
    ) {
        super(transactionManager);
        this.apartmentSharingLinkRepository = apartmentSharingLinkRepository;
        this.applicationProjectionLoader = applicationProjectionLoader;
        this.lightApplicationResponseProjection = lightApplicationResponseProjection;
        this.linkLogService = linkLogService;
    }

    @Override
    public ApplicationModel execute(GetLightApplicationCommand command) {
        checkTransaction();

        ApartmentSharingLink link = apartmentSharingLinkRepository.findValidLinkByToken(command.token(), false)
                .orElseThrow(() -> new ApartmentSharingNotFoundException(command.token().toString()));

        ApplicationProjectionSources sources = this.<ApplicationProjectionSources>executeInTransaction(status ->
                        applicationProjectionLoader.load(link.getApartmentSharing().getId()))
                .orElseThrow();

        // Legacy behavior: a light consultation is always logged
        linkLogService.save(new LinkLog(link.getApartmentSharing(), command.token(), LinkType.LIGHT_APPLICATION, command.ipAddress()));

        return lightApplicationResponseProjection.project(sources);
    }

    public record GetLightApplicationCommand(UUID token, String ipAddress) {
    }
}

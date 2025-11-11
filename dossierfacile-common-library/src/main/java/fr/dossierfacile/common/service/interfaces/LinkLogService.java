package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.service.LinkLogServiceImpl.FirstAndLastVisit;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LinkLogService {
    LinkLog save(LinkLog log);

    Optional<LocalDateTime> getLastVisit(UUID token, ApartmentSharing apartmentSharing);

    FirstAndLastVisit getFirstAndLastVisit(UUID token, ApartmentSharing apartmentSharing);

    long countVisits(UUID token, ApartmentSharing apartmentSharing);

    LinkLog createNewLog(ApartmentSharingLink link, LinkType linkType);
}
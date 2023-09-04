package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.LinkLog;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LinkLogService {
    LinkLog save(LinkLog log);
    Optional<LocalDateTime> getLastVisit(String token, ApartmentSharing apartmentSharing);
}
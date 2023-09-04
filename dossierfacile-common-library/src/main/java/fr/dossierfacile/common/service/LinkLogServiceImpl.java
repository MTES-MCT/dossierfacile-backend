package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.repository.LinkLogRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class LinkLogServiceImpl implements LinkLogService {
    private final LinkLogRepository linkLogRepository;

    public LinkLog save(LinkLog log) {
        return linkLogRepository.save(log);
    }

    public Optional<LocalDateTime> getLastVisit(String token, ApartmentSharing apartmentSharing) {
        List<LinkType> visitLogs = List.of(LinkType.FULL_APPLICATION, LinkType.LIGHT_APPLICATION, LinkType.DOCUMENT);
        return linkLogRepository.findByApartmentSharingAndToken(apartmentSharing, token)
                .stream()
                .filter(log -> visitLogs.contains(log.getLinkType()))
                .sorted(Comparator.comparing(LinkLog::getCreationDate).reversed())
                .map(LinkLog::getCreationDate)
                .findFirst();
    }

}

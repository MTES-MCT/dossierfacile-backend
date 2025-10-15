package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.repository.LinkLogRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class LinkLogServiceImpl implements LinkLogService {
    private final LinkLogRepository linkLogRepository;

    public LinkLog save(LinkLog log) {
        return linkLogRepository.save(log);
    }

    public Optional<LocalDateTime> getLastVisit(UUID token, ApartmentSharing apartmentSharing) {
        List<LinkType> visitLogs = List.of(LinkType.FULL_APPLICATION, LinkType.LIGHT_APPLICATION, LinkType.DOCUMENT);
        return linkLogRepository.findByApartmentSharingAndToken(apartmentSharing, token)
                .stream()
                .filter(log -> visitLogs.contains(log.getLinkType()))
                .sorted(Comparator.comparing(LinkLog::getCreationDate).reversed())
                .map(LinkLog::getCreationDate)
                .findFirst();
    }

    public LinkLog createNewLog(ApartmentSharingLink link, LinkType linkType) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        LinkLog log = LinkLog.builder()
                .token(link.getToken())
                .creationDate(LocalDateTime.now())
                .apartmentSharing(link.getApartmentSharing())
                .linkType(linkType)
                .ipAddress(request.getRemoteAddr())
                .build();
        return linkLogRepository.save(log);
    }

    @Override
    public long countVisits(UUID token, ApartmentSharing apartmentSharing) {
        List<LinkType> visitLogs = List.of(LinkType.FULL_APPLICATION, LinkType.LIGHT_APPLICATION, LinkType.DOCUMENT);
        return linkLogRepository.findByApartmentSharingAndToken(apartmentSharing, token)
            .stream()
            .filter(log -> visitLogs.contains(log.getLinkType()))
            .count();
    }

}

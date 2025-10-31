package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.common.model.ApartmentSharingLinkModel;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static fr.dossierfacile.common.enums.ApartmentSharingLinkType.MAIL;
import static fr.dossierfacile.common.enums.LinkType.*;

@Service
@Slf4j
@AllArgsConstructor
public class ApartmentSharingLinkService {

    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final LinkLogService linkLogService;

    public List<ApartmentSharingLinkModel> getLinksByMail(ApartmentSharing apartmentSharing) {
        return apartmentSharingLinkRepository.findByApartmentSharingAndLinkTypeAndDeletedIsFalse(apartmentSharing, MAIL)
                .stream()
                .map(link -> mapApartmentSharingLink(link, apartmentSharing))
                .toList();
    }

    public List<ApartmentSharingLinkModel> getLinks(ApartmentSharing apartmentSharing) {
        return apartmentSharingLinkRepository.findByApartmentSharingOrderByCreationDate(apartmentSharing)
                .stream()
                .map(link -> mapApartmentSharingLink(link, apartmentSharing))
                .toList();
    }

    private ApartmentSharingLinkModel mapApartmentSharingLink(ApartmentSharingLink link, ApartmentSharing apartmentSharing) {
        LocalDateTime lastVisit = linkLogService.getLastVisit(link.getToken(), apartmentSharing).orElse(null);
        long nbVisits = linkLogService.countVisits(link.getToken(), apartmentSharing);
        return ApartmentSharingLinkModel.builder()
                .id(link.getId())
                .creationDate(link.getCreationDate())
                .ownerEmail(link.getEmail())
                .lastVisit(lastVisit)
                .enabled(!link.isDisabled())
                .deleted(link.isDeleted())
                .fullData(link.isFullData())
                .expirationDate(link.getExpirationDate())
                .title(link.getTitle())
                .type(link.getLinkType().toString())
                .nbVisits(nbVisits)
                .build();
    }

    public void updateStatus(Long linkId, boolean enabled, ApartmentSharing apartmentSharing) {
        var link = apartmentSharingLinkRepository.findByIdAndApartmentSharingAndDeletedIsFalse(linkId, apartmentSharing).orElseThrow(NotFoundException::new);
        link.setDisabled(!enabled);
        linkLogService.createNewLog(link, enabled ? ENABLED_LINK : DISABLED_LINK);
        apartmentSharingLinkRepository.save(link);
    }

    public void delete(Long linkId) {
        var link = apartmentSharingLinkRepository.findById(linkId).orElseThrow(NotFoundException::new);
        log.info("Delete token: " + link.getToken() + " by " + link.getLinkType() + " on apartmentSharing" + link.getApartmentSharing().getId());
        linkLogService.createNewLog(link, DELETED_LINK_TOKEN);
        link.setExpirationDate(LocalDateTime.now());
        link.setDeleted(true);
        apartmentSharingLinkRepository.save(link);
    }

    public void delete(Long linkId, Tenant tenant) {
        boolean hasAccess = tenant.getApartmentSharing().getApartmentSharingLinks().stream()
                .anyMatch(link -> link.getId().equals(linkId));
        if (hasAccess) {
            delete(linkId);
        }
    }

    private boolean isValidLink(ApartmentSharingLink link) {
        return !link.isDeleted() && (link.getExpirationDate() == null || LocalDateTime.now().isBefore(link.getExpirationDate()));
    }

    public void deleteLinks(List<Long> linkIds, Tenant tenant) {
        for (var link : tenant.getApartmentSharing().getApartmentSharingLinks()) {
            if (linkIds.contains(link.getId())) {
                delete(link.getId());
            }
        }
    }

    public void disableValidLinks(Tenant tenant) {
        log.info("Pause all valid links on apartmentSharing" + tenant.getApartmentSharing().getId());
        for (var link : tenant.getApartmentSharing().getApartmentSharingLinks()) {
            if (isValidLink(link) && !link.isDisabled()) {
                link.setDisabled(true);
                apartmentSharingLinkRepository.save(link);
            }
        }
    }

    public void enableValidLinks(Tenant tenant) {
        log.info("Enable all valid links on apartmentSharing" + tenant.getApartmentSharing().getId());
        for (var link : tenant.getApartmentSharing().getApartmentSharingLinks()) {
            if (isValidLink(link) && link.isDisabled()) {
                link.setDisabled(false);
                apartmentSharingLinkRepository.save(link);
            }
        }
    }

    public void regenerateToken(UUID token) {
        var link = apartmentSharingLinkRepository.findByToken(token).orElseThrow(NotFoundException::new);
        linkLogService.save(LinkLog.builder().creationDate(LocalDateTime.now()).linkType(REBUILT_TOKENS).token(token).apartmentSharing(link.getApartmentSharing()).build());
        UUID newToken = UUID.randomUUID();
        log.info("Regenerate token " + token + " to " + newToken);
        link.setToken(newToken);
        apartmentSharingLinkRepository.save(link);
    }

}

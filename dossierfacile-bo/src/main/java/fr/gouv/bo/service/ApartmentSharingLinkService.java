package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@AllArgsConstructor
public class ApartmentSharingLinkService {
    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final LinkLogService linkLogService;

    public void delete(Long linkId) {
        ApartmentSharingLink link = apartmentSharingLinkRepository.findById(linkId).orElseThrow();
        log.info("Delete token: " + link.getToken() + " by " + link.getLinkType() + " on apartmentSharing" + link.getApartmentSharing().getId());
        linkLogService.save(LinkLog.builder().token(link.getToken()).creationDate(LocalDateTime.now()).apartmentSharing(link.getApartmentSharing()).linkType(LinkType.DELETED_LINK_TOKEN).build());
        apartmentSharingLinkRepository.deleteById(linkId);
    }
}

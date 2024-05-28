package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import fr.gouv.bo.repository.BOApartmentSharingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class ApartmentSharingService {
    private final BOApartmentSharingRepository apartmentSharingRepository;
    private final ApartmentSharingCommonService apartmentSharingCommonService;
    private final LinkLogService linkLogService;

    public ApartmentSharing find(Long id) {
        return apartmentSharingCommonService.findById(id).get();
    }

    public void delete(ApartmentSharing apartmentSharing) {
        apartmentSharingRepository.delete(apartmentSharing);
    }

    public void save(ApartmentSharing apartmentSharing) {
        apartmentSharingRepository.save(apartmentSharing);
    }

    @Transactional
    public void resetDossierPdfGenerated(ApartmentSharing apartmentSharing) {
        apartmentSharingCommonService.resetDossierPdfGenerated(apartmentSharing);
    }

    public void refreshUpdateDate(ApartmentSharing apartmentSharing) {
        apartmentSharing.setLastUpdateDate(LocalDateTime.now());
        apartmentSharingRepository.save(apartmentSharing);
    }

    @Transactional
    public void regenerateTokens(Long id) {
        ApartmentSharing apartmentSharing = apartmentSharingCommonService.findById(id).orElseThrow(NotFoundException::new);
        linkLogService.save(LinkLog.builder().creationDate(LocalDateTime.now()).linkType(LinkType.REBUILT_TOKENS).token(apartmentSharing.getToken()).apartmentSharing(apartmentSharing).build());
        String token = UUID.randomUUID().toString();
        log.info("Regenerate token: " + apartmentSharing.getToken() + " to " + token);
        apartmentSharing.setToken(token);
        apartmentSharing.setTokenPublic(UUID.randomUUID().toString());
        apartmentSharingRepository.save(apartmentSharing);
    }
}

package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.gouv.bo.repository.BOApartmentSharingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@Service
public class ApartmentSharingService {
    private final BOApartmentSharingRepository apartmentSharingRepository;
    private final ApartmentSharingCommonService apartmentSharingCommonService;

    public ApartmentSharing find(Long id) {
        return apartmentSharingCommonService.findById(id).get();
    }

    public void delete(ApartmentSharing apartmentSharing) {
        apartmentSharingRepository.delete(apartmentSharing);
    }

    public ApartmentSharing save(ApartmentSharing apartmentSharing) {
        return apartmentSharingRepository.save(apartmentSharing);
    }

    public ApartmentSharing createApartmentSharingFor(Tenant tenant) {
        ApartmentSharing apartmentSharing = new ApartmentSharing(tenant);
        tenant.setApartmentSharing(apartmentSharing);
        return save(apartmentSharing);
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
    public void removeTenant(ApartmentSharing apartmentSharing, Tenant tenant) {
        apartmentSharingCommonService.removeTenant(apartmentSharing, tenant);
    }

}

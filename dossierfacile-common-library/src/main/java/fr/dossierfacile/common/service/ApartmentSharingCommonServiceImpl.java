package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ApartmentSharingCommonServiceImpl implements ApartmentSharingCommonService {
    ApartmentSharingRepository apartmentSharingRepository;
    FileStorageService fileStorageService;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void resetDossierPdfGenerated(ApartmentSharing apartmentSharing) {
        String currentUrl = apartmentSharing.getUrlDossierPdfDocument();
        if (currentUrl != null) {
            fileStorageService.delete(currentUrl);
            apartmentSharing.setUrlDossierPdfDocument(null);
            apartmentSharing.setDossierPdfDocumentStatus(FileStatus.DELETED);
            apartmentSharingRepository.save(apartmentSharing);
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void removeTenant(ApartmentSharing apartmentSharing, Tenant tenant) {
        apartmentSharing.getTenants().remove(tenant);
        apartmentSharing.setApplicationType((apartmentSharing.getNumberOfTenants() >= 2) ? ApplicationType.GROUP : ApplicationType.ALONE);
        resetDossierPdfGenerated(apartmentSharing);
        apartmentSharingRepository.save(apartmentSharing);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void delete(ApartmentSharing apartmentSharing) {
        apartmentSharingRepository.delete(apartmentSharing);
    }

}

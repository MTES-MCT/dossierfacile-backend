package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class ApartmentSharingCommonServiceImpl implements ApartmentSharingCommonService {

    private final ApartmentSharingRepository apartmentSharingRepository;
    private final FileStorageService fileStorageService;
    private final LogService logService;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void resetDossierPdfGenerated(ApartmentSharing apartmentSharing) {
        StorageFile pdfFile = apartmentSharing.getPdfDossierFile();
        apartmentSharing.setPdfDossierFile(null);
        apartmentSharing.setDossierPdfDocumentStatus(FileStatus.DELETED);
        apartmentSharingRepository.save(apartmentSharing);
        fileStorageService.delete(pdfFile);
    }
    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void removeTenant(ApartmentSharing apartmentSharing, Tenant tenant) {
        apartmentSharing.getTenants().remove(tenant);

        ApplicationType newApplicationType = (apartmentSharing.getNumberOfTenants() >= 2) ? ApplicationType.GROUP : ApplicationType.ALONE;
        logService.saveApplicationTypeChangedLog(apartmentSharing.getTenants(), apartmentSharing.getApplicationType(), newApplicationType);
        apartmentSharing.setApplicationType(newApplicationType);

        resetDossierPdfGenerated(apartmentSharing);
        apartmentSharingRepository.save(apartmentSharing);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void delete(ApartmentSharing apartmentSharing) {
        apartmentSharingRepository.delete(apartmentSharing);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Optional<ApartmentSharing> findById(Long apartmentSharingId) {
        return apartmentSharingRepository.findById(apartmentSharingId);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public ApartmentSharing save(ApartmentSharing apartmentSharing) {
        return apartmentSharingRepository.save(apartmentSharing);
    }
}

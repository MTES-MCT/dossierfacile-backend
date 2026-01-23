package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.service.interfaces.ApartmentSharingPdfDossierFileGenerationService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ApartmentSharingPdfDossierFileGenerationServiceImpl implements ApartmentSharingPdfDossierFileGenerationService {
    private final ApartmentSharingCommonService apartmentSharingCommonService;
    private final FileStorageService fileStorageService;

    @Override
    public void complete(Long id, StorageFile file) {
        Optional<ApartmentSharing> optionalApartmentSharing = apartmentSharingCommonService.findById(id);

        // Handle case where ApartmentSharing was deleted during PDF generation
        if (optionalApartmentSharing.isEmpty()) {
            log.warn("ApartmentSharing {} was deleted during PDF generation, marking pdfDossierFile as TO_DELETE", id);
            fileStorageService.delete(file);
            return;
        }

        ApartmentSharing apartmentSharing = optionalApartmentSharing.get();

        // Mark previous pdfDossierFile as TO_DELETE before replacing
        StorageFile previousPdfDossierFile = apartmentSharing.getPdfDossierFile();
        if (previousPdfDossierFile != null) {
            log.warn("Previous pdfDossierFile {} was deleted during PDF generation, marking pdfDossierFile as TO_DELETE", previousPdfDossierFile.getId());
            fileStorageService.delete(previousPdfDossierFile);
        }

        apartmentSharing.setPdfDossierFile(file);
        apartmentSharing.setDossierPdfDocumentStatus(FileStatus.COMPLETED);
        apartmentSharingCommonService.save(apartmentSharing);
    }

    @Override
    public void fail(Long apartmentSharingId, StorageFile file) {
        Optional<ApartmentSharing> optionalApartmentSharing = apartmentSharingCommonService.findById(apartmentSharingId);

        // Handle case where ApartmentSharing was deleted during PDF generation
        if (optionalApartmentSharing.isEmpty()) {
            log.warn("ApartmentSharing {} was deleted during PDF generation, marking pdfDossierFile as TO_DELETE", apartmentSharingId);
            fileStorageService.delete(file);
            return;
        }

        ApartmentSharing apartmentSharing = optionalApartmentSharing.get();
        apartmentSharing.setDossierPdfDocumentStatus(FileStatus.FAILED);
        apartmentSharing.setPdfDossierFile(null);
        apartmentSharingCommonService.save(apartmentSharing);
        fileStorageService.delete(file);
    }

    @Override
    public ApartmentSharing initialize(Long apartmentSharingId) {
        ApartmentSharing apartmentSharing = apartmentSharingCommonService.findById(apartmentSharingId).get();
        if (apartmentSharing.getDossierPdfDocumentStatus() == FileStatus.IN_PROGRESS) {
            log.warn("Dossier PDF for ApartmentSharing with ID [" + apartmentSharing.getId() + "] currently in progress - Do nothing");
            return null;
        }
        apartmentSharing.setDossierPdfDocumentStatus(FileStatus.IN_PROGRESS);
        apartmentSharing = apartmentSharingCommonService.save(apartmentSharing);

        // Load necessary value for generation in sessionContext
        apartmentSharing.getTenants().stream().forEach(
                t -> {
                    t.getDocuments().stream().collect(Collectors.toList());
                    t.getGuarantors().stream().forEach(g -> g.getDocuments().stream().collect(Collectors.toList()));
                }
        );
        return apartmentSharing;
    }
}

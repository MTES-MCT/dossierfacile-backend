package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.repository.ApartmentSharingRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.enums.FileStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApartmentSharingServiceImpl implements ApartmentSharingService {
    private final ApartmentSharingRepository repository;

    @Override
    @Transactional
    public void setDossierPdfDocumentStatus(Long id, FileStatus fileStatus) {
        ApartmentSharing apartmentSharing = repository.getById(id);

        switch (fileStatus){
            case COMPLETED -> {
                if (apartmentSharing.getDossierPdfDocumentStatus() != FileStatus.IN_PROGRESS) {
                    throw new IllegalStateException("Cannot complete a document which are not in progress state");
                }
                if (apartmentSharing.getUrlDossierPdfDocument() == null) {
                    throw new IllegalStateException("Cannot complete a document with null url");
                }
            }
            case FAILED, DELETED -> apartmentSharing.setUrlDossierPdfDocument(null);
        }

        apartmentSharing.setDossierPdfDocumentStatus(fileStatus);
        repository.save(apartmentSharing);
    }

    @Override
    @Transactional
    public ApartmentSharing initialiseApartmentSharingForPdfGeneration(Long apartmentSharingId) {
        ApartmentSharing apartmentSharing = repository.getById(apartmentSharingId);
        if (apartmentSharing.getUrlDossierPdfDocument() != null) {
            log.warn("Dossier PDF for ApartmentSharing with ID [" + apartmentSharing.getId() + "] currently exists - Do nothing");
            return null;
        }
        if (apartmentSharing.getDossierPdfDocumentStatus() == FileStatus.IN_PROGRESS) {
            log.warn("Dossier PDF for ApartmentSharing with ID [" + apartmentSharing.getId() + "] currently in progress - Do nothing");
            return null;
        }
        apartmentSharing.setUrlDossierPdfDocument(apartmentSharing.getToken() + ".pdf");
        apartmentSharing.setDossierPdfDocumentStatus(FileStatus.IN_PROGRESS);

        apartmentSharing = repository.save(apartmentSharing);
        // Load necessary value for generation in sessionContext
        apartmentSharing.getTenants().stream().forEach(
                t -> {
                    t.getDocuments().stream().collect(Collectors.toList());
                    t.getGuarantors().stream().forEach( g -> g.getDocuments().stream().collect(Collectors.toList()));
                }
        );
        return apartmentSharing;
    }
}

package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.garbagecollector.model.apartment.ApartmentSharing;
import fr.dossierfacile.garbagecollector.model.object.Object;
import fr.dossierfacile.garbagecollector.repo.apartment.ApartmentSharingRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.ObjectService;
import fr.dossierfacile.garbagecollector.service.interfaces.OvhService;
import fr.dossierfacile.garbagecollector.transactions.interfaces.ObjectTransactions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteArchivedDocumentService {

    private static final Integer LIMIT_OBJECTS_TO_DELETE = 50;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final ObjectTransactions objectTransactions;
    private final OvhService ovhService;

    @Scheduled(cron="0 0 8 3 * ?")
    public void deleteApartmentSharingPdfTask() {
        //check if there is connection to ovh
        if (ovhService.getObjectStorage() == null) {
            log.warn("No connection to OVH " + "\n");
            return;
        }
        List<ApartmentSharing> apartmentSharings = apartmentSharingRepository.getArchivedAptWithPdf(LIMIT_OBJECTS_TO_DELETE);

        for (ApartmentSharing apartmentSharing : apartmentSharings) {
            try {
                log.info("delete apt " + apartmentSharing.getId());
                log.info("delete  " + apartmentSharing.getUrl_dossier_pdf_document());
                ovhService.delete(apartmentSharing.getUrl_dossier_pdf_document());
                apartmentSharing.setUrl_dossier_pdf_document("");
                apartmentSharingRepository.save(apartmentSharing);
            } catch (Exception e) {
                log.error("Couldn't delete object [" + apartmentSharing.getUrl_dossier_pdf_document() + "] from apartment_sharing [" + apartmentSharing.getId() + "]");
            }
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void deleteDocumentTask() {
        //check if there is connection to ovh
        if (ovhService.getObjectStorage() == null) {
            log.warn("No connection to OVH " + "\n");
            return;
        }
        List<ApartmentSharing> apartmentSharings = apartmentSharingRepository.getArchivedAptWithDocumentWithPdf(LIMIT_OBJECTS_TO_DELETE);

        for (ApartmentSharing apartmentSharing : apartmentSharings) {
            try {
                log.info("delete apt " + apartmentSharing.getId());
                log.info("delete  " + apartmentSharing.getUrl_dossier_pdf_document());
                ovhService.delete(apartmentSharing.getUrl_dossier_pdf_document());
                apartmentSharing.setUrl_dossier_pdf_document("");
                apartmentSharingRepository.save(apartmentSharing);
            } catch (Exception e) {
                log.error("Couldn't delete object [" + apartmentSharing.getUrl_dossier_pdf_document() + "] from apartment_sharing [" + apartmentSharing.getId() + "]");
            }
        }
    }
}

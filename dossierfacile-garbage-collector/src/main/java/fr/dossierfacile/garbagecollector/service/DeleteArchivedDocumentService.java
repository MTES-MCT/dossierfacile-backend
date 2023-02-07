package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.garbagecollector.model.apartment.ApartmentSharing;
import fr.dossierfacile.garbagecollector.model.document.Document;
import fr.dossierfacile.garbagecollector.model.file.File;
import fr.dossierfacile.garbagecollector.repo.apartment.ApartmentSharingRepository;
import fr.dossierfacile.garbagecollector.repo.document.DocumentRepository;
import fr.dossierfacile.garbagecollector.repo.file.FileRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.OvhService;
import fr.dossierfacile.garbagecollector.transactions.interfaces.ObjectTransactions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteArchivedDocumentService {

    private static final Integer LIMIT_OBJECTS_TO_DELETE = 50;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final ObjectTransactions objectTransactions;
    private final OvhService ovhService;

//    @Scheduled(cron="0 0 8 3 * ?")
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

//    @Scheduled(fixedDelay = 5000)
    public void deleteDocumentTask() {
        //check if there is connection to ovh
        if (ovhService.getObjectStorage() == null) {
            log.warn("No connection to OVH " + "\n");
            return;
        }
        List<Document> documents = documentRepository.getArchivedDocumentWithPdf(LIMIT_OBJECTS_TO_DELETE);

        for (Document document : documents) {
            try {
                log.info("delete document " + document.getId());
                log.info("delete  " + document.getName());
                ovhService.delete(document.getName());
                document.setName("");
                documentRepository.save(document);
            } catch (Exception e) {
                log.error("Couldn't delete document [" + document.getName() + "] from document [" + document.getId() + "]");
            }
        }
    }


//    @Scheduled(fixedDelay = 5000)
    public void deleteFileTask() {
        //check if there is connection to ovh
        if (ovhService.getObjectStorage() == null) {
            log.warn("No connection to OVH " + "\n");
            return;
        }
        List<File> files = fileRepository.getArchivedFile(LIMIT_OBJECTS_TO_DELETE);

        for (File file : files) {
            try {
                log.info("delete document " + file.getId());
                log.info("delete  " + file.getPath());
                ovhService.delete(file.getPath());
                if (StringUtils.isNotBlank(file.getPreview())) {
                    ovhService.delete(file.getPreview());
                }
                fileRepository.delete(file);
            } catch (Exception e) {
                log.error("Couldn't delete file [" + file.getPath() + "] from file [" + file.getId() + "]");
            }
        }
    }
}

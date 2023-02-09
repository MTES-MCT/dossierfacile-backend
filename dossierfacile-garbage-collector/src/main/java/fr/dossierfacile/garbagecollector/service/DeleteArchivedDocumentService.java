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

//    @Scheduled(fixedDelay = 2000)
    public void deleteApartmentSharingPdfTask() {
        if(!ovhService.hasConnection()) {
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

//    @Scheduled(fixedDelay = 1000)
    public void deleteDocumentTask() {
        if(!ovhService.hasConnection()) {
            return;
        }
        List<Document> documents = documentRepository.getArchivedDocumentWithPdf(LIMIT_OBJECTS_TO_DELETE);

        documents.parallelStream().forEach((document) -> {
            try {
                log.info("delete document " + document.getId());
                log.info("delete  " + document.getName());
                ovhService.delete(document.getName());
                document.setName("");
                documentRepository.save(document);
            } catch (Exception e) {
                log.error("Couldn't delete document [" + document.getName() + "] from document [" + document.getId() + "]");
            }
        });
    }


//    @Scheduled(fixedDelay = 1000)
    public void deleteFileTask() {
        if(!ovhService.hasConnection()) {
            return;
        }
        List<File> files = fileRepository.getArchivedFile(LIMIT_OBJECTS_TO_DELETE);

        files.parallelStream().forEach((file) -> {
            try {
                log.info("delete file " + file.getId());
                log.info("delete " + file.getPath());
                ovhService.delete(file.getPath());
                if (StringUtils.isNotBlank(file.getPreview())) {
                    ovhService.delete(file.getPreview());
                }
                fileRepository.delete(file);
            } catch (Exception e) {
                log.error("Couldn't delete file [" + file.getPath() + "] from file [" + file.getId() + "]");
            }
        });
    }
}

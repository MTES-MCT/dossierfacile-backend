package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.garbagecollector.model.apartment.GarbageApartmentSharing;
import fr.dossierfacile.garbagecollector.model.document.GarbageDocument;
import fr.dossierfacile.garbagecollector.model.file.GarbageFile;
import fr.dossierfacile.garbagecollector.repo.apartment.GarbageApartmentSharingRepository;
import fr.dossierfacile.garbagecollector.repo.document.DocumentRepository;
import fr.dossierfacile.garbagecollector.repo.file.FileRepository;
import fr.dossierfacile.garbagecollector.service.interfaces.OvhService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteArchivedDocumentService {

    private static final Integer LIMIT_OBJECTS_TO_DELETE = 50;
    private final GarbageApartmentSharingRepository apartmentSharingRepository;
    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final OvhService ovhService;
    private final StorageFileRepository storageFileRepository;

    //    @Scheduled(fixedDelay = 2000)
    public void deleteApartmentSharingPdfTask() {

        List<GarbageApartmentSharing> apartmentSharings = apartmentSharingRepository.getArchivedAptWithPdf(LIMIT_OBJECTS_TO_DELETE);

        for (GarbageApartmentSharing apartmentSharing : apartmentSharings) {
            try {
                log.info("delete apt " + apartmentSharing.getId());
                log.info("delete  " + apartmentSharing.getPdfDossierFile());

                storageFileRepository.delete(apartmentSharing.getPdfDossierFile());
                apartmentSharing.setPdfDossierFile(null);
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.DELETED);
                apartmentSharingRepository.save(apartmentSharing);
            } catch (Exception e) {
                log.error("Couldn't delete object [" + apartmentSharing.getPdfDossierFile() + "] from apartment_sharing [" + apartmentSharing.getId() + "]");
            }
        }
    }

    //    @Scheduled(fixedDelay = 1000)
    public void deleteDocumentTask() {
        if (!ovhService.hasConnection()) {
            return;
        }
        List<GarbageDocument> documents = documentRepository.getArchivedDocumentWithPdf(LIMIT_OBJECTS_TO_DELETE);

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
    public void deleteGuarantorDocumentTask() {
        if (!ovhService.hasConnection()) {
            return;
        }
        List<GarbageDocument> documents = documentRepository.getGuarantorArchivedDocumentWithPdf(LIMIT_OBJECTS_TO_DELETE);

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


//        @Scheduled(fixedDelay = 1000)
    public void deleteFileTask() {
        List<GarbageFile> files = fileRepository.getArchivedFile(LIMIT_OBJECTS_TO_DELETE);

        files.parallelStream().forEach((file) -> {
            try {
                log.info("delete file " + file.getId());
                fileRepository.delete(file);
            } catch (Exception e) {
                log.error("Couldn't delete file [" + file.getId() + "]");
            }
        });
    }

//            @Scheduled(fixedDelay = 1000)
    public void deleteGuarantorFileTask() {
        List<GarbageFile> files = fileRepository.getGuarantorArchivedFile(LIMIT_OBJECTS_TO_DELETE);

        files.parallelStream().forEach((file) -> {
            try {
                log.info("delete file " + file.getId());
                fileRepository.delete(file);
            } catch (Exception e) {
                log.error("Couldn't delete file [" + file.getId() + "]");
            }
        });
    }
}

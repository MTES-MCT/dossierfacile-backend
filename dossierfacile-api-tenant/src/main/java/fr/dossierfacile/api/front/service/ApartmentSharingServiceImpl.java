package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.front.model.MappingFormat;
import fr.dossierfacile.api.front.repository.ApiTenantLogRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.LinkLog;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.LinkType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.mapper.ApartmentSharingMapper;
import fr.dossierfacile.common.mapper.ApplicationBasicMapper;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.mapper.ApplicationLightMapper;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@AllArgsConstructor
@Slf4j
public class ApartmentSharingServiceImpl implements ApartmentSharingService {

    private final ApartmentSharingRepository apartmentSharingRepository;
    private final ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    private final TenantCommonRepository tenantRepository;
    private final ApplicationFullMapper applicationFullMapper;
    private final ApplicationLightMapper applicationLightMapper;
    private final ApplicationBasicMapper applicationBasicMapper;
    private final FileStorageService fileStorageService;
    private final LinkLogService linkLogService;
    private final Producer producer;
    private final ApartmentSharingCommonService apartmentSharingCommonService;
    private final ApiTenantLogRepository tenantLogRepository;
    private final LogService logService;

    @Override
    public ApplicationModel full(String token) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByToken(token).orElse(null);
        if (apartmentSharing == null) {
            Optional<ApartmentSharingLink> apartmentSharingLink = apartmentSharingLinkRepository.findByTokenAndFullDataAndDisabledIsFalse(token, true);
            if (apartmentSharingLink.isEmpty()) {
                throw new ApartmentSharingNotFoundException(token);
            }
            apartmentSharing = apartmentSharingLink.get().getApartmentSharing();
        }
        saveLinkLog(apartmentSharing, token, LinkType.FULL_APPLICATION);
        ApplicationModel applicationModel = applicationFullMapper.toApplicationModel(apartmentSharing);
        applicationModel.setLastUpdateDate(getLastUpdateDate(apartmentSharing));
        return applicationModel;
    }

    @Override
    public ApplicationModel light(String token) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByTokenPublic(token).orElse(null);
        if (apartmentSharing == null) {
            Optional<ApartmentSharingLink> apartmentSharingLink = apartmentSharingLinkRepository.findByTokenAndFullDataAndDisabledIsFalse(token, false);
            if (apartmentSharingLink.isEmpty()) {
                throw new ApartmentSharingNotFoundException(token);
            }
            apartmentSharing = apartmentSharingLink.get().getApartmentSharing();
        }
        saveLinkLog(apartmentSharing, token, LinkType.LIGHT_APPLICATION);
        ApplicationModel applicationModel = applicationLightMapper.toApplicationModel(apartmentSharing);
        applicationModel.setLastUpdateDate(getLastUpdateDate(apartmentSharing));
        return applicationModel;
    }

    private LocalDateTime getLastUpdateDate(ApartmentSharing apartmentSharing) {
        LocalDateTime lastUpdateDate = apartmentSharing.getLastUpdateDate();
        if (apartmentSharing.getStatus() == TenantFileStatus.VALIDATED) {
            Optional<TenantLog> log = tenantLogRepository.findLastValidationLogByApartmentSharing(apartmentSharing.getId());
            if (log.isPresent()) {
                lastUpdateDate = log.get().getCreationDateTime();
            }
        }
        return lastUpdateDate;
    }

    @Override
    public ByteArrayOutputStream downloadFullPdf(String token) throws IOException {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByToken(token)
                .orElseThrow(() -> new ApartmentSharingNotFoundException(token));

        FileStatus status = apartmentSharing.getDossierPdfDocumentStatus() == null ? FileStatus.NONE : apartmentSharing.getDossierPdfDocumentStatus();
        switch (status) {
            case COMPLETED -> {
                ByteArrayOutputStream outputStreamResult = new ByteArrayOutputStream();
                try (InputStream fileIS = fileStorageService.download(apartmentSharing.getPdfDossierFile())) {
                    log.info("Dossier PDF downloaded for ApartmentSharing with ID [" + apartmentSharing.getId() + "]");
                    IOUtils.copy(fileIS, outputStreamResult);
                    saveLinkLog(apartmentSharing, token, LinkType.DOCUMENT);

                } catch (FileNotFoundException e) {
                    log.error("Unable to download Dossier pdf [" + apartmentSharing.getId() + "].");
                    throw e;
                } catch (IOException e) {
                    log.error("Unable to download Dossier pdf [" + apartmentSharing.getId() + "].");
                    throw new UnknownServiceException("Unable to get Full PDF from Storage");
                }
                return outputStreamResult;
            }
            case IN_PROGRESS -> {
                throw new IllegalStateException("Full PDF doesn't exist - FileStatus " + apartmentSharing.getDossierPdfDocumentStatus());
            }
            case FAILED -> {
                throw new FileNotFoundException("Full PDF doesn't exist - FileStatus " + apartmentSharing.getDossierPdfDocumentStatus());
            }
            default -> {
                createFullPdf(token);
                throw new IllegalStateException("Full PDF doesn't exist - create it - FileStatus " + apartmentSharing.getDossierPdfDocumentStatus());
            }
        }
    }

    @Override
    public void resetDossierPdfGenerated(ApartmentSharing apartmentSharing) {
        apartmentSharingCommonService.resetDossierPdfGenerated(apartmentSharing);
    }

    @Override
    public Optional<ApartmentSharing> findById(Long apartmentSharingId) {
        return apartmentSharingRepository.findById(apartmentSharingId);
    }

    private void saveLinkLog(ApartmentSharing apartmentSharing, String token, LinkType linkType) {
        linkLogService.save(new LinkLog(
                apartmentSharing,
                token,
                linkType
        ));
    }

    @Override
    public void createFullPdf(String token) {
        ApartmentSharing apartmentSharing = apartmentSharingRepository.findByToken(token).orElse(null);
        if (apartmentSharing == null) {
            Optional<ApartmentSharingLink> apartmentSharingLink = apartmentSharingLinkRepository.findByTokenAndFullDataAndDisabledIsFalse(token, true);
            if (apartmentSharingLink.isEmpty()) {
                throw new ApartmentSharingNotFoundException(token);
            }
            apartmentSharing = apartmentSharingLink.get().getApartmentSharing();
        }

        checkingAllTenantsInTheApartmentAreValidatedAndAllDocumentsAreNotNull(apartmentSharing.getId(), token);

        FileStatus status = apartmentSharing.getDossierPdfDocumentStatus() == null ? FileStatus.NONE : apartmentSharing.getDossierPdfDocumentStatus();
        switch (status) {
            case COMPLETED -> log.warn("Trying to create Full PDF on completed Status -" + token);
            case IN_PROGRESS -> log.warn("Trying to create Full PDF on in progress Status -" + token);
            default -> {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.NONE);
                producer.generateFullPdf(apartmentSharing.getId());
            }
        }
    }

    @Override
    public void refreshUpdateDate(ApartmentSharing apartmentSharing) {
        apartmentSharing.setLastUpdateDate(LocalDateTime.now());
        apartmentSharingRepository.save(apartmentSharing);
    }

    @Override
    public void removeTenant(ApartmentSharing apartmentSharing, Tenant tenant) {
        apartmentSharingCommonService.removeTenant(apartmentSharing, tenant);
    }

    @Override
    public void delete(ApartmentSharing apartmentSharing) {
        apartmentSharingCommonService.delete(apartmentSharing);
    }

    @Override
    public ByteArrayOutputStream zipDocuments(Tenant tenant) {
        logService.saveLog(LogType.ZIP_DOWNLOAD, tenant.getId());
        final Path tmpDir = Paths.get("tmp");
        String zipFileName = tmpDir + File.separator + "dossier_location_" + tenant.getLastName() + "_" + ThreadLocalRandom.current().nextInt() + ".zip";
        try {
            Files.createDirectories(tmpDir);
            final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));

            for (Tenant t : tenant.getApartmentSharing().getTenants()) {
                addTenantDocumentsToZip(t, outputStream);
            }

            outputStream.close();

            ByteArrayOutputStream outputStreamResult = new ByteArrayOutputStream();
            File f = new File(zipFileName);
            try (InputStream fileIS = new FileInputStream(f)) {
                log.info("Dossier zip downloaded for tenant with ID [" + tenant.getId() + "]");
                IOUtils.copy(fileIS, outputStreamResult);
                return outputStreamResult;
            }
        } catch (IOException e) {
            log.error("Error while zipping", e);
        } finally {
            try {
                Files.delete(Path.of(zipFileName));
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private void addTenantDocumentsToZip(Tenant tenant, ZipOutputStream outputStream) {
        String tenantFolder = tenant.getFullName().replace(" ", "_");
        tenant.getDocuments().forEach(document -> addDocumentToZip(outputStream, document, tenantFolder));
        tenant.getGuarantors().forEach(guarantor -> guarantor.getDocuments().forEach(document -> {
                    String guarantorFolderName = "";
                    if (TypeGuarantor.NATURAL_PERSON.equals( guarantor.getTypeGuarantor())) {
                        guarantorFolderName = File.separator + guarantor.getCompleteName().replace(" ", "_");
                    } else {
                        guarantorFolderName = File.separator + "garant";
                    }
                    addDocumentToZip(outputStream, document, tenantFolder + guarantorFolderName);
        }
        ));
    }

    private void addDocumentToZip(ZipOutputStream outputStream, Document document, String folder) {
        StorageFile watermarkFile = document.getWatermarkFile();
        if (watermarkFile == null) {
            log.error("Error watermark is null : " + document.getId());
            return;
        }
        String filename = folder + File.separator + document.getDocumentCategory().getText() + "_" + document.getId() + ".pdf";
        try (InputStream inputStream = fileStorageService.download(watermarkFile)) {
            addToZipFile(outputStream, filename, inputStream);
        } catch (IOException e) {
            log.error("Error while zipping document : " + document.getId(), e);
        }
    }

    private void addToZipFile(ZipOutputStream zos, String filename, InputStream inputStream) throws IOException {
        ZipEntry zipEntry = new ZipEntry(filename);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = inputStream.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
    }

    @Override
    public List<ApplicationModel> findApartmentSharingByLastUpdateDateAndPartner(LocalDateTime lastUpdateDate, UserApi userApi, int limit, MappingFormat format) {
        ApartmentSharingMapper mapper = (format == MappingFormat.EXTENDED) ? applicationFullMapper : applicationBasicMapper;

        return apartmentSharingRepository.findByLastUpdateDateAndPartner(lastUpdateDate, userApi, PageRequest.of(0, limit)).stream().map(a ->
                mapper.toApplicationModel(a, userApi)).collect(Collectors.toList());
    }

    private void checkingAllTenantsInTheApartmentAreValidatedAndAllDocumentsAreNotNull(long apartmentSharingId, String token) {
        int numberOfTenants = tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(apartmentSharingId);
        if (numberOfTenants > 0) {
            throw new ApartmentSharingUnexpectedException(token);
        }
    }
}

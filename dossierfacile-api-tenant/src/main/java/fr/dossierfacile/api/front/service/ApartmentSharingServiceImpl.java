package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.front.exception.TrigramNotAuthorizedException;
import fr.dossierfacile.api.front.model.MappingFormat;
import fr.dossierfacile.api.front.model.tenant.FullFolderFile;
import fr.dossierfacile.api.front.repository.ApiTenantLogRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.common.mapper.ApartmentSharingMapper;
import fr.dossierfacile.common.mapper.ApplicationBasicMapper;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.mapper.ApplicationLightMapper;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.api.front.util.TrigramUtils;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import fr.dossierfacile.common.service.interfaces.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.*;
import java.io.File;
import java.net.UnknownServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    private final BruteForceProtectionService bruteForceProtectionService;

    @Override
    public void linkExists(UUID token, boolean fullData) {
        Optional<ApartmentSharingLink> apartmentSharingLink = apartmentSharingLinkRepository.findValidLinkByToken(token, true);
        // 1. Check if the link exists and is valid
        if (apartmentSharingLink.isEmpty()) {
            throw new ApartmentSharingNotFoundException(token.toString());
        }

        ApartmentSharingLink link = apartmentSharingLink.get();
        // 2. Check if the link is blocked by brute force protection
        bruteForceProtectionService.checkAndEnforceProtection(link);
    }

    @Override
    public ApplicationModel full(UUID token, String trigram) {
        // 1. Check if the link exists and is valid
        Optional<ApartmentSharingLink> apartmentSharingLink = apartmentSharingLinkRepository.findValidLinkByToken(token, true);
        if (apartmentSharingLink.isEmpty()) {
            throw new ApartmentSharingNotFoundException(token.toString());
        }
        ApartmentSharingLink link = apartmentSharingLink.get();

        // 2. Check if the link is blocked by brute force protection
        bruteForceProtectionService.checkAndEnforceProtection(link);

        // 3. Check if the trigram is present and valid
        ApartmentSharing apartmentSharing = link.getApartmentSharing();

        try {
            validateAndNormalizeTrigram(apartmentSharing, trigram);
        } catch (TrigramNotAuthorizedException e) {
            bruteForceProtectionService.recordFailedAttempt(link);
            throw e;
        }
        
        // 4. Reset the attempts if the trigram is valid
        bruteForceProtectionService.resetAttempts(link);

        // 5. Save log and return the application model
        saveLinkLog(apartmentSharing, token, LinkType.FULL_APPLICATION);
        ApplicationModel applicationModel = applicationFullMapper.toApplicationModelWithToken(apartmentSharing, token);
        applicationModel.setLastUpdateDate(getLastUpdateDate(apartmentSharing));
        return applicationModel;
    }

    public ApplicationModel full(Tenant tenant) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        ApplicationModel applicationModel = applicationFullMapper.toApplicationModel(apartmentSharing);
        applicationModel.setLastUpdateDate(getLastUpdateDate(apartmentSharing));
        return applicationModel;
    }


    @Override
    public ApplicationModel light(UUID token) {
        ApartmentSharing apartmentSharing = findValidApartmentSharing(token, false);
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
    public FullFolderFile downloadFullPdf(UUID token) throws IOException {
        ApartmentSharing apartmentSharing = findValidApartmentSharing(token, true);
        return handlePdfDownloadByStatus(
                apartmentSharing,
                () -> saveLinkLog(apartmentSharing, token, LinkType.DOCUMENT),
                () -> createFullPdf(token)
        );
    }

    @Override
    public void resetDossierPdfGenerated(ApartmentSharing apartmentSharing) {
        apartmentSharingCommonService.resetDossierPdfGenerated(apartmentSharing);
    }

    @Override
    public Optional<ApartmentSharing> findById(Long apartmentSharingId) {
        return apartmentSharingRepository.findById(apartmentSharingId);
    }

    private void saveLinkLog(ApartmentSharing apartmentSharing, UUID token, LinkType linkType) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ipAddress = request.getHeader("X-Real-Ip");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        linkLogService.save(new LinkLog(
                apartmentSharing,
                token,
                linkType,
                ipAddress
        ));
    }

    @Override
    public void createFullPdf(UUID token) {
        ApartmentSharing apartmentSharing = findValidApartmentSharing(token, true);

        checkingAllTenantsInTheApartmentAreValidatedAndAllDocumentsAreNotNull(apartmentSharing.getId(), token.toString());

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

    private ApartmentSharing findValidApartmentSharing(UUID token, boolean fullData) {
        Optional<ApartmentSharingLink> apartmentSharingLink = apartmentSharingLinkRepository.findValidLinkByToken(token, fullData);
        if (apartmentSharingLink.isEmpty()) {
            throw new ApartmentSharingNotFoundException(token.toString());
        }
        return apartmentSharingLink.get().getApartmentSharing();
    }

    boolean validateAndNormalizeTrigram(ApartmentSharing apartmentSharing, String trigram) {
        // Check if trigram is provided
        if (trigram == null || trigram.isBlank()) {
            log.warn("Missing trigram for apartmentSharing [{}]", apartmentSharing.getId());
            throw new TrigramNotAuthorizedException("Trigram is required to access full application");
        }

        // Normalize trigram (strip whitespace and convert to uppercase)
        String normalizedTrigram = trigram.strip().toUpperCase();

        // Get valid trigrams for this apartment sharing (from lastName and preferredName of tenant and account owner)
        List<String> validTrigrams = apartmentSharing.getTenants() == null ? List.of() : apartmentSharing.getTenants().stream()
                .flatMap(tenant -> Stream.of(
                        tenant.getLastName(),
                        tenant.getPreferredName(),
                        tenant.getUserLastName(),
                        tenant.getUserPreferredName()))
                .map(TrigramUtils::compute)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        // Check if the provided trigram matches any valid trigram
        boolean trigramMatches = validTrigrams.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(normalizedTrigram));

        if (!trigramMatches) {
            log.warn("Unauthorized trigram [{}] for apartmentSharing [{}]. Valid trigrams: {}", normalizedTrigram, apartmentSharing.getId(), validTrigrams);    
            throw new TrigramNotAuthorizedException("Trigram does not match any tenant for this application");
        }

        return true;
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
    public FullFolderFile zipDocuments(Tenant tenant) {
        logService.saveLog(LogType.ZIP_DOWNLOAD, tenant.getId());
        final Path tmpDir = Paths.get("tmp");
        var fileName = getFullFolderName(tenant.getApartmentSharing(), "zip");
        String zipFileName = tmpDir + File.separator + fileName;
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
                return FullFolderFile.builder()
                        .fileOutputStream(outputStreamResult)
                        .fileName(fileName)
                        .build();
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


    @Override
    public void createFullPdfForTenant(Tenant tenant) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        if (apartmentSharing == null) {
            throw new ApartmentSharingNotFoundException("No apartment sharing found for tenant");
        }

        checkingAllTenantsInTheApartmentAreValidatedAndAllDocumentsAreNotNull(apartmentSharing.getId(), null);

        FileStatus status = apartmentSharing.getDossierPdfDocumentStatus() == null ? FileStatus.NONE : apartmentSharing.getDossierPdfDocumentStatus();
        switch (status) {
            case COMPLETED -> log.warn("Trying to create Full PDF on completed Status");
            case IN_PROGRESS -> log.warn("Trying to create Full PDF on in progress Status");
            default -> {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.NONE);
                producer.generateFullPdf(apartmentSharing.getId());
            }
        }
    }


    @Override
    public FullFolderFile downloadFullPdfForTenant(Tenant tenant) throws IOException {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        if (apartmentSharing == null) {
            throw new ApartmentSharingNotFoundException("No apartment sharing found for tenant");
        }
        return handlePdfDownloadByStatus(
                apartmentSharing,
                () -> logService.saveLog(LogType.PDF_DOWNLOAD, tenant.getId()),
                () -> createFullPdfForTenant(tenant)
        );
    }

    private void addTenantDocumentsToZip(Tenant tenant, ZipOutputStream outputStream) {
        String tenantFolder = tenant.getFullName().replace(" ", "_");
        tenant.getDocuments().forEach(document -> addDocumentToZip(outputStream, document, tenantFolder));
        tenant.getGuarantors().forEach(guarantor -> guarantor.getDocuments().forEach(document -> {
            String guarantorFolderName = "";
            if (TypeGuarantor.NATURAL_PERSON.equals(guarantor.getTypeGuarantor())) {
                guarantorFolderName = File.separator + guarantor.getCompleteName().replace(" ", "_");
            } else {
                guarantorFolderName = File.separator + "garant";
            }
            addDocumentToZip(outputStream, document, tenantFolder + guarantorFolderName);
        }));
    }

    private void addDocumentToZip(ZipOutputStream outputStream, Document document, String folder) {
        StorageFile watermarkFile = document.getWatermarkFile();
        if (watermarkFile == null) {
            log.error("Error watermark is null : " + document.getId());
            return;
        }
        String fileName = folder + File.separator + document.getDocumentName();
        try (InputStream inputStream = fileStorageService.download(watermarkFile)) {
            addToZipFile(outputStream, fileName, inputStream);
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
            if (token != null) {
                throw new ApartmentSharingUnexpectedException(token);
            } else {
                throw new ApartmentSharingUnexpectedException();
            }
        }
    }


    private String getFullFolderName(ApartmentSharing apartmentSharing, String extension) {
        var fileName = String.format("DossierFacile.%s", extension);
        var ownerTenant = apartmentSharing.getOwnerTenant();
        if (ownerTenant.isEmpty()) {
            return fileName;
        }

        if (apartmentSharing.getApplicationType() == ApplicationType.ALONE) {
            return String.format("DossierFacile_%s.%s", ownerTenant.get().getNormalizedName(), extension);
        }
        if (apartmentSharing.getApplicationType() == ApplicationType.COUPLE) {
            return String.format("DossierFacile_%s_%s.%s", ownerTenant.get().getNormalizedName(), "couple", extension);
        }
        if (apartmentSharing.getApplicationType() == ApplicationType.GROUP) {
            return String.format("DossierFacile_%s_%s.%s", ownerTenant.get().getNormalizedName(), "collocation", extension);
        }

        return fileName;
    }

    private FullFolderFile handlePdfDownloadByStatus(
            ApartmentSharing apartmentSharing,
            Runnable onSuccessLog,
            Runnable onCreatePdf) throws IOException {

        FileStatus status = apartmentSharing.getDossierPdfDocumentStatus() == null
                ? FileStatus.NONE
                : apartmentSharing.getDossierPdfDocumentStatus();

        switch (status) {
            case COMPLETED -> {
                ByteArrayOutputStream outputStreamResult = new ByteArrayOutputStream();
                try (InputStream fileIS = fileStorageService.download(apartmentSharing.getPdfDossierFile())) {
                    log.info("Dossier PDF downloaded for ApartmentSharing with ID [" + apartmentSharing.getId() + "]");
                    IOUtils.copy(fileIS, outputStreamResult);
                    onSuccessLog.run();

                } catch (FileNotFoundException e) {
                    log.error("Unable to download Dossier pdf [" + apartmentSharing.getId() + "].");
                    throw e;
                } catch (IOException e) {
                    log.error("Unable to download Dossier pdf [" + apartmentSharing.getId() + "].");
                    throw new UnknownServiceException("Unable to get Full PDF from Storage");
                }

                return FullFolderFile.builder()
                        .fileOutputStream(outputStreamResult)
                        .fileName(getFullFolderName(apartmentSharing, "pdf"))
                        .build();
            }
            case IN_PROGRESS -> {
                throw new IllegalStateException("Full PDF doesn't exist - FileStatus " + apartmentSharing.getDossierPdfDocumentStatus());
            }
            case FAILED -> {
                throw new FileNotFoundException("Full PDF doesn't exist - FileStatus " + apartmentSharing.getDossierPdfDocumentStatus());
            }
            default -> {
                onCreatePdf.run();
                throw new IllegalStateException("Full PDF doesn't exist - FileStatus " + apartmentSharing.getDossierPdfDocumentStatus());
            }
        }
    }

}
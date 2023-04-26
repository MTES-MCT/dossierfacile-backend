package fr.dossierfacile.common.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.dossierfacile.common.entity.AccountDeleteLog;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.mapper.TenantCommonMapper;
import fr.dossierfacile.common.model.apartment_sharing.TenantModel;
import fr.dossierfacile.common.repository.AccountDeleteLogCommonRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.common.repository.FileCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import fr.dossierfacile.common.utils.LocalDateTimeTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class TenantCommonServiceImpl implements TenantCommonService {

    private final AccountDeleteLogCommonRepository accountDeleteLogRepository;
    private final TenantCommonMapper tenantCommonMapper;
    private final FileStorageService fileStorageService;
    private final ApartmentSharingRepository apartmentSharingRepository;
    private final DocumentCommonRepository documentRepository;
    private final FileCommonRepository fileRepository;
    private final TenantCommonRepository tenantCommonRepository;

    @Override
    public void recordAndDeleteTenantData(Long tenantId) {
        Tenant tenant = tenantCommonRepository.findOneById(tenantId);
        TenantModel tenantModel = tenantCommonMapper.toTenantModel(tenant);
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter());
        Gson gson = builder.create();

        accountDeleteLogRepository.save(
                AccountDeleteLog.builder()
                        .userId(tenantModel.getId())
                        .deletionDate(LocalDateTime.now())
                        .jsonProfileBeforeDeletion(gson.toJson(tenantModel))
                        .build()
        );

        Optional<ApartmentSharing> apartmentSharing = apartmentSharingRepository.findByTenant(tenant.getId());
        if (apartmentSharing.isEmpty()) {
            return;
        }

        if (StringUtils.isNotBlank(apartmentSharing.get().getUrlDossierPdfDocument())) {
            try {
                fileStorageService.delete(tenant.getApartmentSharing().getUrlDossierPdfDocument());
                tenant.getApartmentSharing().setUrlDossierPdfDocument("");
                apartmentSharingRepository.save(tenant.getApartmentSharing());
            } catch (Exception e) {
                log.error("Couldn't delete object [" + tenant.getApartmentSharing().getUrlDossierPdfDocument() + "] from apartment_sharing [" + tenant.getApartmentSharing().getId() + "]");
            }
        }

        Optional.ofNullable(tenant.getDocuments())
                .orElse(new ArrayList<>())
                .forEach(this::deleteFilesFromStorage);
        Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .forEach(guarantor -> Optional.ofNullable(guarantor.getDocuments())
                        .orElse(new ArrayList<>())
                        .forEach(this::deleteFilesFromStorage)
                );
    }


    @Override
    @Transactional
    public void addDeleteLogIfMissing(Long tenantId) {
        Tenant tenant = tenantCommonRepository.findOneById(tenantId);
        if (tenant == null) {
            log.info("Tenant already deleted");
            return;
        }
        TenantModel tenantModel = tenantCommonMapper.toTenantModel(tenant);
        List<AccountDeleteLog> accountDeleteLog = accountDeleteLogRepository.findByUserId(tenantModel.getId());
        if (accountDeleteLog.isEmpty()) {
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter());
            Gson gson = builder.create();
            accountDeleteLogRepository.save(AccountDeleteLog.builder().userId(tenantModel.getId()).deletionDate(LocalDateTime.now()).jsonProfileBeforeDeletion(gson.toJson(tenantModel)).build());
        }
    }
    private void deleteFilesFromStorage(Document document) {
        List<File> files = document.getFiles();
        if (files != null && !files.isEmpty()) {
            log.info("Removing files from storage of document with id [" + document.getId() + "]");
            fileStorageService.delete(files.stream().map(File::getPath).collect(Collectors.toList()));
            files.forEach(file -> fileStorageService.delete(file.getPreview()));
        }
        if (document.getName() != null && !document.getName().isBlank()) {
            log.info("Removing document from storage with path [" + document.getName() + "]");
            fileStorageService.delete(document.getName());
        }
        documentRepository.delete(document);
    }
    @Override
    public Tenant findByKeycloakId(String keycloakId) {
        return tenantCommonRepository.findByKeycloakId(keycloakId);
    }
}

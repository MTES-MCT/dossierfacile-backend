package fr.gouv.bo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.gouv.bo.mapper.TenantMapperForOperator;
import fr.gouv.bo.model.tenant.TenantModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Temporary code for extract data for Manufacture POC
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ToolsService {

    private final TenantMapperForOperator tenantMapper;
    private final TenantCommonRepository tenantCommonRepository;
    private final StorageFileRepository storageFileRepository;
    private final FileStorageService fileStorageService;

    public void generateLastTreatedDossiers(int count) throws Exception {
        // First generate the "count" dossier
        for ( int i = 0 ; i < (count/10) ; i++) {
            Pageable pageable = PageRequest.of(
                    i,
                    10,
                    Sort.by(Sort.Direction.DESC, "lastUpdateDate")
            );
            List<Tenant> tenants = tenantCommonRepository.findTenantsToExtract(pageable);

            // then create the zip
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String zipName = "zip-" + UUID.randomUUID() +".zip";
            // Création d'un flux de sortie vers le fichier ZIP
            FileOutputStream fos = new FileOutputStream(zipName);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (Tenant tenant : tenants) {

                TenantModel t = tenantMapper.toTenantModel(tenant);
                JsonElement je = JsonParser.parseString(objectMapper.writeValueAsString(t));

                // on traite les fichiers
                for (Document d : tenant.getDocuments()) {
                    for(fr.dossierfacile.common.entity.File f : d.getFiles()) {
                        try (InputStream fileInputStream = fileStorageService.download(f.getStorageFile())) {

                            zos.putNextEntry(new ZipEntry("tid-" + t.getId() + "/doc-" + d.getId() + "/" + f.getStorageFile().getPath()));

                            byte[] buffer = new byte[4096];
                            int length;
                            while ((length = fileInputStream.read(buffer)) > 0) {
                                zos.write(buffer, 0, length);
                            }
                            zos.closeEntry();
                        } catch (Exception e) {
                            log.error("Error manufacture", e);
                            zos.closeEntry();
                        }
                        log.info("Fichier File traitré id:" + f.getId());
                    }
                }
                // guarantor
                for (Guarantor g : tenant.getGuarantors()) {
                    for (Document d : g.getDocuments()) {
                        for(fr.dossierfacile.common.entity.File f : d.getFiles()) {
                            try (InputStream fileInputStream = fileStorageService.download(f.getStorageFile())) {

                                zos.putNextEntry(new ZipEntry("tid-" + t.getId() + "/doc-" + d.getId() + "/" + f.getStorageFile().getPath()));

                                byte[] buffer = new byte[4096];
                                int length;
                                while ((length = fileInputStream.read(buffer)) > 0) {
                                    zos.write(buffer, 0, length);
                                }
                                zos.closeEntry();
                            } catch (Exception e) {
                                log.error("Error manufacture", e);
                                zos.closeEntry();
                            }
                            log.info("Fichier File traitré id:" + f.getId());
                        }
                    }
                }
                // on traite le tenant en json
                String tenantString = gson.toJson(je);
                byte[] bytes = tenantString.getBytes();
                InputStream inputStream = new ByteArrayInputStream(bytes);

                try {
                    zos.putNextEntry(new ZipEntry("tid-" + t.getId() + ".json"));
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Error manufacture", e);
                    zos.closeEntry();
                }
            }

            zos.close();

            log.info("ZIP NAME = " + zipName);
            //then push the zip
            try {
                File file = new File(zipName);

                try (InputStream zipInputStream = new FileInputStream(file)) {
                    StorageFile sfile = StorageFile.builder()
                            .name("zip-manufacture.zip")
                            .contentType("application/zip")
                            .build();
                    fileStorageService.upload(zipInputStream, sfile);
                }
            } catch (IOException e) {
                log.error("zip probleme");
            }
        }

    }

    public List<StorageFile> getFiles() {
        return storageFileRepository.findAllByName("zip-manufacture.zip");
    }
}

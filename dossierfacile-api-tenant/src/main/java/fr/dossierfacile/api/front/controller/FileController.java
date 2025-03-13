package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.FileService;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
@Slf4j
public class FileController {
    private static final String FILE_NO_EXIST = "The file does not exist";
    private final FileService fileService;
    private final AuthenticationFacade authenticationFacade;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var tenant = authenticationFacade.getLoggedTenant();
        fileService.delete(id, tenant);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/resource/{id}", produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public void getPrivateFileAsByteArray(HttpServletResponse response, @PathVariable Long id) {
        Tenant tenant = authenticationFacade.getLoggedTenant();

        var file = getFileForTenantOrCouple(id, tenant);

        try (InputStream in = fileStorageService.download(file.getStorageFile())) {
            response.setContentType(file.getStorageFile().getContentType());
            IOUtils.copy(in, response.getOutputStream());
        } catch (final java.io.FileNotFoundException e) {
            log.error(FILE_NO_EXIST, e);
            response.setStatus(404);
        } catch (IOException e) {
            log.error("File cannot be downloaded - 408 - Too long?", e);
            response.setStatus(404);
        }
    }

    @GetMapping(value = "/preview/{fileId}")
    public void getPreviewFromFileIdAsByteArray(HttpServletResponse response, @PathVariable Long fileId) {
        Tenant tenant = authenticationFacade.getLoggedTenant();

        var file = getFileForTenantOrCouple(fileId, tenant);

        try (InputStream in = fileStorageService.download(file.getPreview())) {
            response.setContentType(file.getPreview().getContentType());
            IOUtils.copy(in, response.getOutputStream());
        } catch (final java.io.FileNotFoundException e) {
            log.error(FILE_NO_EXIST, e);
            response.setStatus(404);
        } catch (IOException e) {
            log.error("File cannot be downloaded - 408 - Too long?", e);
            response.setStatus(408);
        }
    }

    // We use this method to allow the tenant to show and download the files uploaded by the other tenant in the couple
    private File getFileForTenantOrCouple(Long fileId, Tenant tenant) throws FileNotFoundException {
        Optional<File> optionalFile;

        if (tenant.getApartmentSharing().getApplicationType() == ApplicationType.COUPLE) {
            optionalFile = fileRepository.findByIdForAppartmentSharing(fileId, tenant.getApartmentSharing().getId());
        } else {
            optionalFile = fileRepository.findByIdForTenant(fileId, tenant.getId());
        }

        return optionalFile.orElseThrow(() -> new FileNotFoundException(fileId));
    }
}

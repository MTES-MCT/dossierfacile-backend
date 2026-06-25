package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.application.exception.ModelNotFoundException;
import fr.dossierfacile.api.front.application.exception.UnauthorizedException;
import fr.dossierfacile.api.front.application.usecase.tenant.TenantDeleteFileUseCase;
import fr.dossierfacile.api.front.security.KeycloakId;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.FileService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.utils.FileUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

// @TODO Need to move this controller inside the package : fr.dossierfacile.api.front.infrastructure.controller

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
@Slf4j
public class FileController {
    private static final String FILE_NO_EXIST = "The file does not exist";
    private final FileService fileService;
    private final AuthenticationFacade authenticationFacade;
    private final FileStorageService fileStorageService;
    private final TenantDeleteFileUseCase tenantDeleteFileUseCase;

    @ApiOperation(value = "Delete a file", notes = "Deletes a file associated with a tenant or their guarantor.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "File deleted successfully"),
            @ApiResponse(code = 401, message = "Unauthorized: JWT token missing or invalid"),
            @ApiResponse(code = 403, message = "Forbidden: Tenant is not authorized to delete this file"),
            @ApiResponse(code = 404, message = "Not Found: File, Tenant, or Apartment Sharing not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @KeycloakId String keycloakId) {
        try {
            tenantDeleteFileUseCase.execute(new TenantDeleteFileUseCase.TenantDeleteFileCommand(id, keycloakId));
            return ResponseEntity.ok().build();
        } catch (ModelNotFoundException e) {
            log.error("Model not found when deleting file with id: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (UnauthorizedException e) {
            log.error("Unauthorized access when deleting file with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping(value = "/resource/{id}", produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public void getPrivateFileAsByteArray(HttpServletResponse response, @PathVariable Long id) {
        Tenant tenant = authenticationFacade.getLoggedTenant();

        var file = fileService.getFileForTenantOrCouple(id, tenant);

        try (InputStream in = fileStorageService.download(file.getStorageFile())) {
            FileUtility.streamFileToResponse(in, file.getStorageFile().getContentType(),
                    file.getStorageFile().getName(), true, response);
        } catch (final java.io.FileNotFoundException e) {
            log.error(FILE_NO_EXIST, e);
            response.setStatus(404);
        } catch (IOException e) {
            log.error("File cannot be downloaded - 408 - Too long?", e);
            response.setStatus(408);
        }
    }

    @GetMapping(value = "/preview/{fileId}")
    public void getPreviewFromFileIdAsByteArray(HttpServletResponse response, @PathVariable Long fileId) {
        Tenant tenant = authenticationFacade.getLoggedTenant();

        var file = fileService.getFileForTenantOrCouple(fileId, tenant);

        try (InputStream in = fileStorageService.download(file.getPreview())) {
            FileUtility.streamFileToResponse(in, file.getPreview().getContentType(),
                    file.getPreview().getName(), true, response);
        } catch (final java.io.FileNotFoundException e) {
            log.error(FILE_NO_EXIST, e);
            response.setStatus(404);
        } catch (IOException e) {
            log.error("File cannot be downloaded - 408 - Too long?", e);
            response.setStatus(408);
        }
    }
}

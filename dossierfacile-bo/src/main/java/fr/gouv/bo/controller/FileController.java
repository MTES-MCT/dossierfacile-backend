package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.SharedFileService;
import fr.gouv.bo.repository.DocumentRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
@Controller
@Slf4j
public class FileController {

    private static final String FILE_NO_EXIST = "The file does not exist";

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final SharedFileService fileService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/files/{id}")
    public void getOriginalFileAsByteArray(HttpServletResponse response, @PathVariable Long id) {
        fileService.findById(id).ifPresentOrElse(
                file -> streamStorageFile(file.getStorageFile(), response),
                () -> {
                    log.error(FILE_NO_EXIST);
                    response.setStatus(404);
                }
        );
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @GetMapping("/files/{id}/preview")
    public void getPreviewFileAsByteArray(HttpServletResponse response, @PathVariable Long id) {
        fileService.findById(id).ifPresentOrElse(
                file -> {
                    if (file.getPreview() == null) {
                        response.setStatus(404);
                        return;
                    }
                    streamStorageFile(file.getPreview(), response);
                },
                () -> {
                    log.error(FILE_NO_EXIST);
                    response.setStatus(404);
                }
        );
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @GetMapping("/documents/{name:.+}")
    public void getDocumentAsByteArray(HttpServletResponse response, @PathVariable String name) {
        documentRepository.findByName(name).ifPresentOrElse(
                document -> streamStorageFile(document.getWatermarkFile(), response),
                () -> {
                    log.error(FILE_NO_EXIST);
                    response.setStatus(404);
                }
        );
    }

    private void streamStorageFile(StorageFile storageFile, HttpServletResponse response) {
        try (InputStream in = fileStorageService.download(storageFile)) {
            response.setContentType(storageFile.getContentType());
            IOUtils.copy(in, response.getOutputStream());
        } catch (final FileNotFoundException e) {
            log.error(FILE_NO_EXIST, e);
            response.setStatus(404);
        } catch (final IOException e) {
            log.error("Unable to download file", e);
            response.setStatus(408);
        }
    }
}

package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.SharedFileService;
import fr.gouv.bo.exception.DocumentNotFoundException;
import fr.gouv.bo.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletResponse;
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
                file -> {
                    try (InputStream in = fileStorageService.download(file.getStorageFile())) {
                        response.setContentType(file.getStorageFile().getContentType());
                        IOUtils.copy(in, response.getOutputStream());
                    } catch (final FileNotFoundException e) {
                        log.error(FILE_NO_EXIST, e);
                        response.setStatus(404);
                    } catch (final IOException e) {
                        log.error("Unable to download file", e);
                        response.setStatus(408);
                    }
                }, () -> {
                    log.error("File not found in Database");
                    response.setStatus(404);
                }
        );
    }

    /**
     * This endpoint does not allow decrypting protected file
     */
    @GetMapping("/documents/{name:.+}")
    public void getFileAsByteArray(HttpServletResponse response, @PathVariable String name) {
        Document document = documentRepository.findByName(name).orElseThrow(() -> new DocumentNotFoundException(name));

        try (InputStream in = fileStorageService.download(document.getWatermarkFile())) {
            response.setContentType(document.getWatermarkFile().getContentType());
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

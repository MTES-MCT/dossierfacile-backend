package fr.gouv.bo.controller;

import fr.dossierfacile.common.service.interfaces.SharedFileService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;

@RequiredArgsConstructor
@Controller
@Slf4j
public class FileController {

    private static final String FILE_NO_EXIST = "The file does not exist";

    private final FileStorageService fileStorageService;
    private final SharedFileService fileService;

    @GetMapping("/tenants_files/{fileName:.+}")
    public void getImageAsByteArray(HttpServletResponse response, @PathVariable String fileName) {
        Key key = fileService.findByPath(fileName).map(f -> f.getKey()).orElse(null);

        try (InputStream in = fileStorageService.download(fileName, key)) {
            if (fileName.endsWith(".pdf")) {
                response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                response.setContentType(MediaType.IMAGE_JPEG_VALUE);
            } else {
                response.setContentType(MediaType.IMAGE_PNG_VALUE);
            }
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

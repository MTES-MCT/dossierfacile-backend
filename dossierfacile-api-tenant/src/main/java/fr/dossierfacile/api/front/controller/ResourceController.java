package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.OvhService;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.openstack4j.model.storage.object.SwiftObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ResourceController {

    private static final String FILE_NO_EXIST = "The file does not exist";

    private final OvhService ovhService;

    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final AuthenticationFacade authenticationFacade;

    @GetMapping("/document/{documentName:.+}")
    public void getPdfWatermarkedAsByteArray(HttpServletResponse response, @PathVariable String documentName) {
        documentRepository.findByName(documentName).orElseThrow(() -> new DocumentNotFoundException(documentName));
        SwiftObject object = ovhService.get(documentName);
        if (object != null) {
            try (InputStream in = object.download().getInputStream()) {
                response.setContentType(MediaType.APPLICATION_PDF_VALUE);
                IOUtils.copy(in, response.getOutputStream());
            } catch (final IOException e) {
                log.error(FILE_NO_EXIST);
                response.setStatus(404);
            }
        } else {
            log.error(FILE_NO_EXIST);
            response.setStatus(404);
        }
    }

    @GetMapping("/file/{id}")
    public void getPrivateFileAsByteArray(HttpServletResponse response, @PathVariable Long id) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        File file = fileRepository.findByIdAndTenant(id, tenant.getId()).orElseThrow(() -> new FileNotFoundException(id));
        String fileName = file.getPath();
        SwiftObject object = ovhService.get(file.getPath());
        if (object != null) {
            try (InputStream in = object.download().getInputStream()) {
                if (fileName.endsWith(".pdf")) {
                    response.setContentType(MediaType.APPLICATION_PDF_VALUE);
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                } else {
                    response.setContentType(MediaType.IMAGE_PNG_VALUE);
                }
                IOUtils.copy(in, response.getOutputStream());
            } catch (final IOException e) {
                log.error(FILE_NO_EXIST);
                response.setStatus(404);
            }
        } else {
            log.error(FILE_NO_EXIST);
            response.setStatus(404);
        }
    }
}

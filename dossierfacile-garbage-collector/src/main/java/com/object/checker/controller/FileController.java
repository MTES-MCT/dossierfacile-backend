package com.object.checker.controller;

import com.object.checker.service.interfaces.OvhService;
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

@RequiredArgsConstructor
@Controller
@Slf4j
public class FileController {

    private static final String FILE_NO_EXIST = "The file does not exist";

    private final OvhService ovhService;

    @GetMapping("/tenants_files/{fileName:.+}")
    public void getImageAsByteArray(HttpServletResponse response, @PathVariable String fileName) {
        SwiftObject object = ovhService.get(fileName);
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

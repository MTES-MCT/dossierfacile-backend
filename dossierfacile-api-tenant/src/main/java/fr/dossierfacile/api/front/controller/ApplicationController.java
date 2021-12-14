package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {
    private static final String DOCUMENT_NOT_EXIST = "The document does not exist";
    private final ApartmentSharingService apartmentSharingService;

    @GetMapping("/full/{token}")
    public ResponseEntity<ApplicationModel> full(@PathVariable String token) {
        ApplicationModel applicationModel = apartmentSharingService.full(token);
        return ok(applicationModel);
    }

    @GetMapping("/light/{token}")
    public ResponseEntity<ApplicationModel> light(@PathVariable String token) {
        ApplicationModel applicationModel = apartmentSharingService.light(token);
        return ok(applicationModel);
    }

    @GetMapping(value = "/fullPdf/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadFullPdf(@PathVariable("token") String token, HttpServletResponse response) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = apartmentSharingService.fullPdf(token);
            if (byteArrayOutputStream.size() > 0) {
                response.setHeader("Content-Disposition", "attachment; filename=" + UUID.randomUUID() + ".pdf");
                response.setHeader("Content-Type", MediaType.APPLICATION_PDF_VALUE);
                response.getOutputStream().write(byteArrayOutputStream.toByteArray());
            } else {
                log.error(DOCUMENT_NOT_EXIST);
                response.setStatus(404);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e.getCause());
        }
    }
}

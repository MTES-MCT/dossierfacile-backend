package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {
    private static final String DOCUMENT_NOT_EXIST = "The document does not exist";
    private final ApartmentSharingService apartmentSharingService;

    @GetMapping(value = "/full/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplicationModel> full(@PathVariable String token) {
        ApplicationModel applicationModel = apartmentSharingService.full(token);
        return ok(applicationModel);
    }

    @GetMapping(value = "/light/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
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
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/fullPdf/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<String> createFullPdf(@PathVariable("token") String token) {
        try {
            apartmentSharingService.createFullPdf(token);
            return accepted().build();
        } catch (ApartmentSharingNotFoundException e) {
            log.error(e.getMessage(), e.getCause());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ApartmentSharingUnexpectedException e) {
            log.error(e.getMessage(), e.getCause());
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}

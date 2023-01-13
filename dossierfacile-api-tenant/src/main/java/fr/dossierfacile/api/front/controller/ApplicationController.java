package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLogTime;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

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

    @MethodLogTime
    @GetMapping(value = "/fullPdf/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadFullPdf(@PathVariable("token") String token, HttpServletResponse response) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = apartmentSharingService.downloadFullPdf(token);
            if (byteArrayOutputStream.size() > 0) {
                response.setHeader("Content-Disposition", "attachment; filename=" + UUID.randomUUID() + ".pdf");
                response.setHeader("Content-Type", MediaType.APPLICATION_PDF_VALUE);
                response.getOutputStream().write(byteArrayOutputStream.toByteArray());
            } else {
                log.error(DOCUMENT_NOT_EXIST);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (ApartmentSharingNotFoundException e) {
            log.error(e.getMessage(), e.getCause());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (IllegalStateException e) {
            log.warn("ApartmentSharing full pdf in not available yet");
            try {
                response.sendError(HttpServletResponse.SC_CONFLICT, "File is not yet available retry later");
            } catch (IOException ex) {
                log.error("Something wrong on response status enrichment", ex);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e.getCause());
            try {
                response.sendError(HttpServletResponse.SC_CONFLICT, "File is not available - check status");
            } catch (IOException ex) {
                log.error("Something wrong on response status enrichment", ex);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

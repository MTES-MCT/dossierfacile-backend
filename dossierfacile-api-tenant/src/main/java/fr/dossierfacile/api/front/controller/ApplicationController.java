package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLogTime;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.front.model.tenant.ApplicationAnalysisStatusResponse;
import fr.dossierfacile.api.front.model.tenant.DocumentAnalysisStatusResponse;
import fr.dossierfacile.api.front.model.tenant.FullFolderFile;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.DocumentServiceImpl;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final AuthenticationFacade authenticationFacade;

    @RequestMapping(value = "/full/{token}", method = RequestMethod.HEAD, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> headFull(@PathVariable UUID token) {
        apartmentSharingService.linkExists(token, true);
        return ok().build();
    }

    @GetMapping(value = "/full/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplicationModel> full(@PathVariable UUID token,
                                                 @RequestHeader(value = "X-Tenant-Trigram", required = true) String trigramHeader) {
        Tenant tenant = null;
        try {
            tenant = authenticationFacade.getLoggedTenant();
        } finally {
            if (tenant == null) {
                log.info("Anonymous request to get full application");
            } else {
                log.info("Authenticated request to get full application, tenantId: {}", tenant.getId());
            }
        }
        return ok(apartmentSharingService.full(token, trigramHeader, tenant));
    }

    @GetMapping(value = "/light/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplicationModel> light(@PathVariable UUID token) {
        ApplicationModel applicationModel = apartmentSharingService.light(token);
        return ok(applicationModel);
    }

    @MethodLogTime
    @GetMapping(value = "/fullPdf/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadFullPdf(@PathVariable UUID token, HttpServletResponse response) {
        handlePdfDownload(() -> apartmentSharingService.downloadFullPdf(token), response);
    }

    @PostMapping(value = "/fullPdf/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<String> createFullPdf(@PathVariable UUID token) {
        return handlePdfCreation(() -> apartmentSharingService.createFullPdf(token));
    }

    @GetMapping(value = "/current-tenant/full", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplicationModel> fullForLoggedTenant() {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        ApplicationModel applicationModel = apartmentSharingService.full(tenant);
        return ok(applicationModel);
    }

    @PostMapping(value = "/current-tenant/fullPdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<String> createFullPdfForLoggedTenant() {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        return handlePdfCreation(() -> apartmentSharingService.createFullPdfForTenant(tenant));
    }

    @MethodLogTime
    @GetMapping(value = "/current-tenant/fullPdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadFullPdf(HttpServletResponse response) {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        handlePdfDownload(() -> apartmentSharingService.downloadFullPdfForTenant(tenant), response);
    }

    @MethodLogTime
    @GetMapping(value = "/zip")
    public void downloadFullZip(HttpServletResponse response) {
        try {
            Tenant tenant = authenticationFacade.getLoggedTenant();
            FullFolderFile fullFolderFile = apartmentSharingService.zipDocuments(tenant);
            if (fullFolderFile.getFileOutputStream().size() > 0) {
                response.setHeader("Access-Control-Expose-Headers", "Content-Disposition, Content-Type");
                response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", fullFolderFile.getFileName()));
                response.setHeader("Content-Type", "application/zip");
                response.setHeader("X-Robots-Tag", "noindex");
                response.getOutputStream().write(fullFolderFile.getFileOutputStream().toByteArray());
            } else {
                log.error(DOCUMENT_NOT_EXIST);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e.getCause());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/analysis-status")
    public ResponseEntity<ApplicationAnalysisStatusResponse> getAnalysisStatus() {
        var tenant = authenticationFacade.getLoggedTenant();
        var response = apartmentSharingService.getFullAnalysisStatus(tenant);
        return ok(response);
    }

    private void handlePdfDownload(PdfDownloadSupplier downloadSupplier, HttpServletResponse response) {
        try {
            FullFolderFile pdfFile = downloadSupplier.get();
            if (pdfFile.getFileOutputStream().size() > 0) {
                response.setHeader("Access-Control-Expose-Headers", "Content-Disposition, Content-Type");
                response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", pdfFile.getFileName()));
                response.setHeader("Content-Type", MediaType.APPLICATION_PDF_VALUE);
                response.setHeader("X-Robots-Tag", "noindex");
                response.getOutputStream().write(pdfFile.getFileOutputStream().toByteArray());
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

    private ResponseEntity<String> handlePdfCreation(PdfCreateSupplier createSupplier) {
        try {
            createSupplier.execute();
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

    @FunctionalInterface
    private interface PdfDownloadSupplier {
        FullFolderFile get() throws IOException;
    }

    @FunctionalInterface
    private interface PdfCreateSupplier {
        void execute() throws ApartmentSharingNotFoundException, ApartmentSharingUnexpectedException;
    }
}

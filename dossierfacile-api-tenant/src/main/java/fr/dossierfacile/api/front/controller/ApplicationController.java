package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLogTime;
import fr.dossierfacile.api.front.application.usecase.application.CheckApplicationLinkUseCase;
import fr.dossierfacile.api.front.application.usecase.application.CheckApplicationLinkUseCase.CheckApplicationLinkCommand;
import fr.dossierfacile.api.front.application.usecase.application.GetFullApplicationUseCase;
import fr.dossierfacile.api.front.application.usecase.application.GetFullApplicationUseCase.GetFullApplicationCommand;
import fr.dossierfacile.api.front.application.usecase.application.GetLightApplicationUseCase;
import fr.dossierfacile.api.front.application.usecase.application.GetLightApplicationUseCase.GetLightApplicationCommand;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.front.model.tenant.ApplicationAnalysisStatusResponse;
import fr.dossierfacile.api.front.model.tenant.FullFolderFile;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.utils.FileUtility;
import fr.dossierfacile.logging.util.LoggerUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {
    private static final String DOCUMENT_NOT_EXIST = "The document does not exist";
    private static final String CONTENT_TYPE_ZIP = "application/zip";
    private final ApartmentSharingService apartmentSharingService;
    private final AuthenticationFacade authenticationFacade;
    private final FileStorageService fileStorageService;
    private final GetLightApplicationUseCase getLightApplicationUseCase;
    private final GetFullApplicationUseCase getFullApplicationUseCase;
    private final CheckApplicationLinkUseCase checkApplicationLinkUseCase;

    @RequestMapping(value = "/full/{token}", method = RequestMethod.HEAD, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> headFull(@PathVariable UUID token) {
        checkApplicationLinkUseCase.execute(new CheckApplicationLinkCommand(token));
        return ok().build();
    }

    @GetMapping(value = "/full/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplicationModel> full(@PathVariable UUID token,
                                                 @RequestHeader(value = "X-Tenant-Trigram", required = true) String trigramHeader,
                                                 HttpServletRequest request) {
        // TODO : migrate getLoggedTenant to use new keycloak sync   
        Tenant tenant = null;
        try {
            tenant = authenticationFacade.getLoggedTenant();
        } catch (Exception e) {
            log.info("Anonymous request to get full application");
        } finally {
            if (tenant != null) {
                log.info("Authenticated request to get full application, tenantId: {}", tenant.getId());
            }
        }
        Long loggedTenantApartmentSharingId = tenant != null && tenant.getApartmentSharing() != null
                ? tenant.getApartmentSharing().getId()
                : null;
        return ok(getFullApplicationUseCase.execute(new GetFullApplicationCommand(
                token, trigramHeader, loggedTenantApartmentSharingId, LoggerUtil.getRealIp(request))));
    }

    @GetMapping(value = "/light/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplicationModel> light(@PathVariable UUID token, HttpServletRequest request) {
        ApplicationModel applicationModel = getLightApplicationUseCase.execute(
                new GetLightApplicationCommand(token, LoggerUtil.getRealIp(request)));
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

    @GetMapping(value = "/links/{token}/documents/{documentName:.+}", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadDocumentByLink(@PathVariable UUID token,
                                       @PathVariable String documentName,
                                       HttpServletResponse response) {
        Document document;
        try {
            document = apartmentSharingService.findDocumentByLink(token, documentName);
        } catch (ApartmentSharingNotFoundException e) {
            log.error(e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try (InputStream in = fileStorageService.download(document.getWatermarkFile())) {
            FileUtility.streamFileToResponse(in, MediaType.APPLICATION_PDF_VALUE, document.getDocumentName(), true, response);
        } catch (FileNotFoundException e) {
            log.error(DOCUMENT_NOT_EXIST);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (IOException e) {
            log.error("Cannot download file", e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
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
                try (InputStream in = new ByteArrayInputStream(fullFolderFile.getFileOutputStream().toByteArray())) {
                    FileUtility.streamFileToResponse(in, CONTENT_TYPE_ZIP, fullFolderFile.getFileName(), false, response);
                }
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
                try (InputStream in = new ByteArrayInputStream(pdfFile.getFileOutputStream().toByteArray())) {
                    FileUtility.streamFileToResponse(in, MediaType.APPLICATION_PDF_VALUE, pdfFile.getFileName(), false, response);
                }
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

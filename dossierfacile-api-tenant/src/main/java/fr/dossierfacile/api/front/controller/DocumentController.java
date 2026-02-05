package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.form.CommentAnalysisForm;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/document")
public class DocumentController {
    private static final String FILE_NO_EXIST = "The file does not exist";
    private final DocumentService documentService;
    private final AuthenticationFacade authenticationFacade;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final TenantMapper tenantMapper;
    private final TenantService tenantService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var tenant = authenticationFacade.getLoggedTenant();
        documentService.delete(id, tenant);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/resource/{documentName:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void getPdfWatermarkedAsByteArray(HttpServletResponse response, @PathVariable String documentName) {
        Document document = documentRepository.findFirstByName(documentName).orElseThrow(() -> new DocumentNotFoundException(documentName));

        try (InputStream in = fileStorageService.download(document.getWatermarkFile())) {
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition, Content-Type");
            var ownerName = getDocumentOwnerNormalizeName(document);
            String fileName;
            if (ownerName.isEmpty()) {
                fileName = document.getDocumentName();
            } else {
                fileName = ownerName + "_" + document.getDocumentName();
            }
            ContentDisposition contentDisposition = ContentDisposition.inline()
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build();
            response.setHeader("Content-Disposition", contentDisposition.toString());
            response.setHeader("X-Robots-Tag", "noindex");
            IOUtils.copy(in, response.getOutputStream());
        } catch (FileNotFoundException e) {
            log.error(FILE_NO_EXIST);
            response.setStatus(404);
        } catch (IOException e) {
            log.error("Cannot download file", e);
            response.setStatus(404);
        }
    }

    @PreAuthorize("hasPermissionOnTenant(#commentAnalysisForm.tenantId)")
    @PostMapping(value = "/commentAnalysis", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> commentAnalysis(@RequestBody CommentAnalysisForm commentAnalysisForm) {
        var tenant = authenticationFacade.getTenant(commentAnalysisForm.getTenantId());
        try {
            tenantService.addCommentAnalysis(tenant, commentAnalysisForm.getDocumentId(), commentAnalysisForm.getComment());
        } catch (Exception e) {
            return badRequest().build();
        }
        if (commentAnalysisForm.getTenantId() != null) {
            return ok(tenantMapper.toTenantModel(authenticationFacade.getTenant(null), null));
        }
        return ok(tenantMapper.toTenantModel(tenant, null));
    }

    private String getDocumentOwnerNormalizeName(Document document) {
        if (document.getTenant() != null) {
            return document.getTenant().getNormalizedName();
        } else {
            if (document.getGuarantor() != null) {
                if (document.getGuarantor().getTypeGuarantor() == TypeGuarantor.ORGANISM) {
                    return "";
                }
                return "garant_" + document.getGuarantor().getNormalizedName();
            }
        }
        return "";
    }
}

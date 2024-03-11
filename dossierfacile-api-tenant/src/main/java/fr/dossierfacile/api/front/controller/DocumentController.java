package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.form.CommentAnalysisForm;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.group.Dossier;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.print.attribute.standard.Media;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
    @PostMapping(value="/commentAnalysis", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> commentAnalysis(@RequestBody CommentAnalysisForm commentAnalysisForm) {
        var tenant = authenticationFacade.getTenant(commentAnalysisForm.getTenantId());
        try {
            tenantService.addCommentAnalysis(tenant, commentAnalysisForm.getDocumentId(), commentAnalysisForm.getComment());
        } catch (Exception e) {
            return badRequest().build();
        }
        return ok(tenantMapper.toTenantModel(tenant));
    }
}

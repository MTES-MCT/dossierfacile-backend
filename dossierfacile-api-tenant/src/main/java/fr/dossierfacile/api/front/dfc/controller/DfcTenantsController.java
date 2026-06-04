package fr.dossierfacile.api.front.dfc.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLogTime;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.ListMetadata;
import fr.dossierfacile.api.front.model.ResponseWrapper;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.model.TenantUpdate;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.utils.FileUtility;
import fr.dossierfacile.logging.util.LoggerUtil;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping(DfcTenantsController.PATH)
@Slf4j
@MethodLogTime
public class DfcTenantsController {
    static final String PATH = "/dfc/api/v1/tenants";
    private final ClientAuthenticationFacade clientAuthenticationFacade;
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final TenantPermissionsService tenantPermissionsService;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    @ApiOperation(value = "Gets a list of tenants associated",
            notes = "Result is ordered by last_update_date. Use 'after' parameter to define a starting point")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper<List<TenantUpdate>, ListMetadata>> list(@RequestParam(value = "after", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
                                                                                  @RequestParam(value = "limit", defaultValue = "1000") Long limit,
                                                                                  @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted,
                                                                                  @RequestParam(value = "includeRevoked", defaultValue = "false") boolean includeRevoked
    ) {
        UserApi userApi = clientAuthenticationFacade.getClient();
        List<TenantUpdate> result = tenantService.findTenantUpdateByLastUpdateAndPartner(after, userApi, limit, includeDeleted, includeRevoked);

        LocalDateTime nextTimeToken = result.isEmpty() ? after : result.get(result.size() - 1).getLastUpdateDate();

        String nextLink = UriComponentsBuilder.fromPath(PATH)
                .queryParam("limit", limit)
                .queryParam("after", nextTimeToken)
                .queryParam("includeDeleted", includeDeleted)
                .queryParam("includeRevoked", includeRevoked)
                .build().encode().toUriString();

        return ok(ResponseWrapper.<List<TenantUpdate>, ListMetadata>builder()
                .metadata(ListMetadata.builder()
                        .limit(limit)
                        .resultCount(result.size())
                        .nextLink(nextLink)
                        .build())
                .data(result)
                .build());
    }

    @ApiOperation(value = "Gets tenant by ID")
    @GetMapping(value = "/{tenantId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectedTenantModel> get(@PathVariable Long tenantId) {
        Tenant tenant = tenantService.findById(tenantId);
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!tenantPermissionsService.clientCanAccess(clientAuthenticationFacade.getKeycloakClientId(), tenant.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ok(tenantMapper.toTenantModelDfc(tenant, clientAuthenticationFacade.getClient()));
    }

    @ApiOperation(value = "Downloads a single watermarked document of a tenant, identified by its UUID",
            notes = "Requires SCOPE_dfc + SCOPE_dfc-documents")
    @GetMapping(value = "/{tenantId}/documents/{documentUuid}", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadDocument(@PathVariable Long tenantId,
                                 @PathVariable String documentUuid,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        Tenant tenant = tenantService.findById(tenantId);
        if (tenant == null || tenant.getApartmentSharing() == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        // the tenant must have agreed to share with this specific partner
        String keycloakClientId = clientAuthenticationFacade.getKeycloakClientId();
        if (!tenantPermissionsService.clientCanAccess(keycloakClientId, tenantId)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        // Same scope as the public share-link endpoint (/api/application/links/{token}/documents/{name}):
        // any document belonging to the tenant's apartment sharing - co-tenants and guarantors included.
        Document document = documentRepository.findByNameForApartmentSharing(documentUuid, tenant.getApartmentSharing().getId())
                .orElse(null);
        if (document == null || document.getWatermarkFile() == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        try (InputStream in = fileStorageService.download(document.getWatermarkFile())) {
            log.info("DFC document download: client={}, tenantId={}, documentUuid={}, ip={}",
                    keycloakClientId, tenantId, documentUuid, LoggerUtil.getRealIp(request));
            FileUtility.streamFileToResponse(in, MediaType.APPLICATION_PDF_VALUE, document.getDocumentName(), true, response);
        } catch (IOException e) {
            log.error("Cannot download DFC document {} for tenant {}", documentUuid, tenantId, e);
            response.setStatus(HttpStatus.NOT_FOUND.value());
        }
    }
}

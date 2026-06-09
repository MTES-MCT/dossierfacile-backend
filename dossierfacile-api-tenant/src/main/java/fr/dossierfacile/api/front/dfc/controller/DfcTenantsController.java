package fr.dossierfacile.api.front.dfc.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLogTime;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.ListMetadata;
import fr.dossierfacile.api.front.model.ResponseWrapper;
import fr.dossierfacile.api.front.dfc.service.DfcDocumentService;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    private final DfcDocumentService dfcDocumentService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "List the partner's tenants",
            description = "Returns the tenants that have consented to share their apartment sharing information with the authenticated partner, "
                    + "ordered by last update date. Use 'after' parameter to define a starting point")
    @ApiResponse(responseCode = "200", description = "Page of tenant updates with pagination metadata")
    @ApiResponse(responseCode = "400", description = "Invalid query parameter (e.g. malformed `after` date)", content = @Content)
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Access token is missing the `dfc` scope or is not a client-credentials token", content = @Content)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper<List<TenantUpdate>, ListMetadata>> list(
            @Parameter(description = "Return only tenants updated strictly after this date.", example = "2026-01-31T10:30:00.000Z")
            @RequestParam(value = "after", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
            @Parameter(description = "Maximum number of tenants to return.", example = "1000")
            @RequestParam(value = "limit", defaultValue = "1000") Long limit,
            @Parameter(description = "Include tenants whose account has been deleted")
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted,
            @Parameter(description = "Include tenants who have revoked their consent.")
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

    @Operation(summary = "Get a tenant's full profile")
    @ApiResponse(responseCode = "200", description = "The tenant's connected profile")
    @ApiResponse(responseCode = "400", description = "Invalid path parameter (e.g. `tenantId` is not a number)", content = @Content)
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Access token is missing the `dfc` scope, or the tenant has not consented to this partner", content = @Content)
    @ApiResponse(responseCode = "404", description = "No tenant found for this id", content = @Content)
    @GetMapping(value = "/{tenantId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectedTenantModel> get(
            @Parameter(description = "Tenant id", example = "413", required = true)
            @PathVariable Long tenantId) {
        Tenant tenant = tenantService.findById(tenantId);
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!tenantPermissionsService.clientCanAccess(clientAuthenticationFacade.getKeycloakClientId(), tenant.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ok(tenantMapper.toTenantModelDfc(tenant, clientAuthenticationFacade.getClient()));
    }

    @Operation(summary = "Download a tenant document",
            description = "Streams the watermarked document belonging to the tenant's apartment sharing, identified by the document token."
                    + "Requires the `dfc` and `dfc-documents` scopes, and the tenant must have consented to this partner.")
    @ApiResponse(responseCode = "200", description = "The watermarked document",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "400", description = "Invalid path parameter (e.g. `tenantId` is not a number)", content = @Content)
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Access token is missing the `dfc`/`dfc-documents` scopes, or the tenant has not consented to this partner", content = @Content)
    @ApiResponse(responseCode = "404", description = "Unknown tenant, or the document does not belong to this tenant's apartment sharing (or has no watermarked version yet)", content = @Content)
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded for document downloads", content = @Content)
    @GetMapping(value = "/{tenantId}/documents/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadDocument(
            @Parameter(description = "Tenant id", example = "413", required = true)
            @PathVariable Long tenantId,
            @Parameter(description = "Document token, as exposed in `documents[].token` of the tenant profile", required = true)
            @PathVariable String token,
            HttpServletRequest request,
            HttpServletResponse response) {
        String keycloakClientId = clientAuthenticationFacade.getKeycloakClientId();
        Document document = dfcDocumentService.getAuthorizedDocument(token, tenantId, keycloakClientId);

        try (InputStream in = fileStorageService.download(document.getWatermarkFile())) {
            log.info("DFC document download: client={}, tenantId={}, token={}, ip={}",
                    keycloakClientId, tenantId, token, LoggerUtil.getRealIp(request));
            FileUtility.streamFileToResponse(in, MediaType.APPLICATION_PDF_VALUE, document.getDocumentName(), true, response);
        } catch (IOException e) {
            log.error("Cannot download DFC document {} for tenant {}", token, tenantId, e);
            response.setStatus(HttpStatus.NOT_FOUND.value());
        }
    }
}

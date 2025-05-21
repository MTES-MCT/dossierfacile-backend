package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.exception.ResendLinkTooShortException;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.ApartmentSharingLinkModel;
import fr.dossierfacile.common.service.ApartmentSharingLinkService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/application/links")
@AllArgsConstructor
public class ApartmentSharingLinkController {

    private final AuthenticationFacade authenticationFacade;
    private final ApartmentSharingLinkService apartmentSharingLinkService;
    private final TenantService tenantService;

    @ApiOperation(value = "Get apartment sharing links", notes = "Retrieves the list of apartment sharing links for the logged-in tenant.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Links retrieved successfully", response = LinksResponse.class),
            @ApiResponse(code = 403, message = "Forbidden: JWT token missing or invalid scope")
    })
    @GetMapping
    public ResponseEntity<LinksResponse> getApartmentSharingLinks() {
        ApartmentSharing apartmentSharing = authenticationFacade.getLoggedTenant().getApartmentSharing();
        List<ApartmentSharingLinkModel> linksByMail = apartmentSharingLinkService.getLinksByMail(apartmentSharing);
        return ResponseEntity.ok(new LinksResponse(linksByMail));
    }

    @ApiOperation(value = "Update apartment sharing link status", notes = "Updates the status of an apartment sharing link.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Status updated successfully"),
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 403, message = "Forbidden: JWT token missing or invalid scope or email unverified"),
            @ApiResponse(code = 404, message = "Link not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateApartmentSharingLinksStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        ApartmentSharing apartmentSharing = authenticationFacade.getLoggedTenant().getApartmentSharing();
        apartmentSharingLinkService.updateStatus(id, enabled, apartmentSharing);
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Resend apartment sharing link", notes = "Resends an apartment sharing link to the tenant.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Link resent successfully"),
            @ApiResponse(code = 403, message = "Forbidden: JWT token missing or invalid scope or email unverified or apartment sharing not in tenant"),
            @ApiResponse(code = 429, message = "Failed because the 1 hour delay is not respected"),
            @ApiResponse(code = 500, message = "link is disabled or call too soon or error when sending email")
    })
    @PostMapping("/{id}/resend")
    public ResponseEntity<Void> resendApartmentSharingLink(@PathVariable Long id) {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        tenantService.resendLink(id, tenant);
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Delete apartment sharing link", notes = "Deletes an apartment sharing link.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Link deleted successfully"),
            @ApiResponse(code = 403, message = "Forbidden: JWT token missing or invalid scope or email unverified"),
            @ApiResponse(code = 500, message = "link is disabled or call too soon or error when sending email")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApartmentSharingLink(@PathVariable Long id) {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        apartmentSharingLinkService.delete(id, tenant);
        return ResponseEntity.ok().build();
    }

    @Data
    @AllArgsConstructor
    public static class LinksResponse {
        List<ApartmentSharingLinkModel> links;
    }

}

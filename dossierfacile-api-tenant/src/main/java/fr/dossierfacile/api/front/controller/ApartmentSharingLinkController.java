package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.ApartmentSharingLinkModel;
import fr.dossierfacile.common.service.ApartmentSharingLinkService;
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

    @GetMapping
    public ResponseEntity<LinksResponse> getApartmentSharingLinks() {
        ApartmentSharing apartmentSharing = authenticationFacade.getLoggedTenant().getApartmentSharing();
        List<ApartmentSharingLinkModel> linksByMail = apartmentSharingLinkService.getLinksByMail(apartmentSharing);
        return ResponseEntity.ok(new LinksResponse(linksByMail));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateApartmentSharingLinksStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        ApartmentSharing apartmentSharing = authenticationFacade.getLoggedTenant().getApartmentSharing();
        apartmentSharingLinkService.updateStatus(id, enabled, apartmentSharing);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<Void> resendApartmentSharingLink(@PathVariable Long id) {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        tenantService.resendLink(id, tenant);
        return ResponseEntity.ok().build();
    }

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

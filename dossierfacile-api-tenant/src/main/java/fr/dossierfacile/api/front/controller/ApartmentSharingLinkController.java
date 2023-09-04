package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.ApartmentSharingLinkModel;
import fr.dossierfacile.common.service.ApartmentSharingLinkService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/application/links")
@AllArgsConstructor
public class ApartmentSharingLinkController {

    private final AuthenticationFacade authenticationFacade;
    private final ApartmentSharingLinkService apartmentSharingLinkService;

    @GetMapping
    public ResponseEntity<LinksResponse> getApartmentSharingLinks() {
        ApartmentSharing apartmentSharing = authenticationFacade.getLoggedTenant().getApartmentSharing();
        List<ApartmentSharingLinkModel> linksByMail = apartmentSharingLinkService.getLinksByMail(apartmentSharing);
        return ResponseEntity.ok(new LinksResponse(linksByMail));
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

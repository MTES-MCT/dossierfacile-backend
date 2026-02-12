package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLogTime;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;


@RestController
@AllArgsConstructor
@RequestMapping("/api-partner/apartmentSharing")
@Validated
@MethodLogTime
public class ApiPartnerApartmentSharingController {
    private final ClientAuthenticationFacade clientAuthenticationFacade;
    private final ApartmentSharingService apartmentSharingService;
    private final UserApiService userApiService;
    private final ApplicationFullMapper applicationFullMapper;

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplicationModel> getApartmentSharing(@PathVariable("id") Long apartmentSharingId) {

        UserApi userApi = clientAuthenticationFacade.getClient();
        ApartmentSharing apartSharing = apartmentSharingService.findById(apartmentSharingId).orElseThrow(() -> new ApartmentSharingNotFoundException("applicaton is not found"));

        // access rules - at least one tenant is linked to the apartmentSharing
        boolean hasReadAccess = this.userApiService.anyTenantIsLinked(userApi, apartSharing.getTenants());
        if (!hasReadAccess) {
            return status(HttpStatus.FORBIDDEN).build();
        }

        return ok(applicationFullMapper.toApplicationModel(apartSharing, userApi));
    }
}
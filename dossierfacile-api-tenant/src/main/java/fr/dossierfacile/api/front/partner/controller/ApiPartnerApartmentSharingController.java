package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLog;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.model.ListMetadata;
import fr.dossierfacile.api.front.model.ResponseWrapper;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;


@RestController
@AllArgsConstructor
@RequestMapping("/api-partner/apartmentSharing")
@Validated
@MethodLog
public class ApiPartnerApartmentSharingController {
    private final AuthenticationFacade authenticationFacade;
    private final ApartmentSharingService apartmentSharingService;
    private final UserApiService userApiService;

    private final ApplicationFullMapper applicationFullMapper;

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplicationModel> getApartmentSharing(@PathVariable("id") Long apartmentSharingId) {

        Optional<UserApi> userApi = this.userApiService.findByName(authenticationFacade.getKeycloakClientId());
        ApartmentSharing apartSharing = apartmentSharingService.findById(apartmentSharingId).orElseThrow(() -> new ApartmentSharingNotFoundException("applicaton is not found"));

        // access rules - at least one tenant is linked to the apartmentSharing
        boolean hasReadAccess = this.userApiService.anyTenantIsAssociated(userApi.get(), apartSharing.getTenants());
        if (!hasReadAccess) {
            return status(HttpStatus.FORBIDDEN).build();
        }

        return ok(applicationFullMapper.toApplicationModel(apartSharing));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper<List<ApplicationModel>, ListMetadata>> listAll(@RequestParam(value = "after", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
                                                                                         @RequestParam(value = "limit", defaultValue = "100") Long limit,
                                                                                         @RequestParam(value = "orderBy", defaultValue = "LAST_UPDATE_DATE") ApiPartnerTenantController.TenantSortType orderBy
    ) {
        Optional<UserApi> userApi = this.userApiService.findByName(authenticationFacade.getKeycloakClientId());
        List<ApplicationModel> result;
        LocalDateTime nextTimeToken;
        if (orderBy == ApiPartnerTenantController.TenantSortType.LAST_UPDATE_DATE) {
            result = apartmentSharingService.findApartmentSharingByLastUpdateDateAndPartner(after, userApi.get(), limit);
            nextTimeToken = (result.size() == 0) ? after : result.get(result.size() - 1).getLastUpdateDate();
        } else {
            throw new IllegalArgumentException();
        }

        String nextLink = "/api-partner/apartmentSharing?limit=" + limit + "&orderBy=" + orderBy + "&after=" + nextTimeToken;
        return ok(ResponseWrapper.<List<ApplicationModel>, ListMetadata>builder()
                .metadata(ListMetadata.builder()
                        .limit(limit)
                        .resultCount(result.size())
                        .nextLink(nextLink)
                        .build())
                .data(result)
                .build());
    }
}
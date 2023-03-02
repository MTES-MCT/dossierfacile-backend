package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLog;
import fr.dossierfacile.api.front.form.SubscriptionApartmentSharingOfTenantForm;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.model.TenantUpdate;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@RestController
@AllArgsConstructor
@RequestMapping("/api-partner/tenant")
@MethodLog
public class ApiPartnerTenantController {

    private final AuthenticationFacade authenticationFacade;
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final UserService userService;
    private final UserApiService userApiService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TenantUpdate>> list(@RequestParam("lastUpdateSince") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastUpdateSince,
                                                   @RequestParam(value = "lastUpdateBefore", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastUpdateBefore) {
        Optional<UserApi> userApi = this.userApiService.findByName(authenticationFacade.getKeycloakClientId());
        if (lastUpdateBefore == null){
            lastUpdateBefore = LocalDateTime.now();
        }
        return ok(tenantService.findTenantUpdateByLastUpdateIntervalAndPartner(lastUpdateSince, lastUpdateBefore, userApi.get()));
    }

    @PreAuthorize("clientHasPermissionOnTenant(#tenantId)")
    @GetMapping(value = {"/{tenantId}/profile"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> profile(@PathVariable Long tenantId) {
        Tenant tenant = authenticationFacade.getTenant(tenantId);
        return ok(tenantMapper.toTenantModel(tenant));
    }

    @PreAuthorize("clientHasPermissionOnTenant(#tenantId)")
    @PostMapping(value = "/{tenantId}/subscribe/{token}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> subscribeTenant(@PathVariable("token") String propertyToken,
                                                @Validated @RequestBody SubscriptionApartmentSharingOfTenantForm subscriptionApartmentSharingOfTenantForm,
                                                @PathVariable Long tenantId) {
        Tenant tenant = authenticationFacade.getTenant(tenantId);
        tenantService.subscribeApartmentSharingOfTenantToPropertyOfOwner(propertyToken, subscriptionApartmentSharingOfTenantForm, tenant);
        return ok().build();
    }

    @PreAuthorize("clientHasPermissionOnTenant(#tenantId)")
    @DeleteMapping("/{tenantId}/deleteCoTenant/{id}")
    public ResponseEntity<Void> deleteCoTenant(@PathVariable Long id, @PathVariable Long tenantId) {
        Tenant tenant = authenticationFacade.getTenant(tenantId);
        return (userService.deleteCoTenant(tenant, id) ? ok() : status(HttpStatus.UNAUTHORIZED)).build();
    }
}

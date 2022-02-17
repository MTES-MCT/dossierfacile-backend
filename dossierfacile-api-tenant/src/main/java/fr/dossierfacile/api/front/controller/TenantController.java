package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.form.PartnerForm;
import fr.dossierfacile.api.front.form.SubscriptionApartmentSharingOfTenantForm;
import fr.dossierfacile.api.front.mapper.PropertyOMapper;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.property.PropertyOModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.PropertyService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@RestController
@AllArgsConstructor
@RequestMapping("/api/tenant")
public class TenantController {

    private final AuthenticationFacade authenticationFacade;
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final PropertyService propertyService;
    private final PropertyOMapper propertyMapper;
    private final UserService userService;

    @GetMapping(value = "/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> profile() {
        Tenant tenant = authenticationFacade.getTenant(null);
        tenantService.updateLastLoginDate(tenant);
        return ok(tenantMapper.toTenantModel(tenant));
    }

    @PostMapping(value = "/subscribe/{token}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> subscribeApartmentSharingOfTenantToPropertyOfOwner(@PathVariable("token") String propertyToken, @Validated @RequestBody SubscriptionApartmentSharingOfTenantForm subscriptionApartmentSharingOfTenantForm) {
        Tenant tenant = authenticationFacade.getTenant(null);
        tenantService.subscribeApartmentSharingOfTenantToPropertyOfOwner(propertyToken, subscriptionApartmentSharingOfTenantForm, tenant);
        return ok().build();
    }

    @GetMapping(value = "/property/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PropertyOModel> getInfoOfPropertyAndOwner(@PathVariable("token") String propertyToken) {
        Property property = propertyService.getPropertyByToken(propertyToken);
        return ok(propertyMapper.toPropertyModel(property));
    }

    @DeleteMapping("/deleteCoTenant/{id}")
    public ResponseEntity<Void> deleteCoTenant(@PathVariable Long id) {
        Tenant tenant = authenticationFacade.getTenant(null);
        return (userService.deleteCoTenant(tenant, id) ? ok() : status(HttpStatus.UNAUTHORIZED)).build();
    }

    @PostMapping(value = "/linkTenantToPartner", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> linkTenantToPartner(@Validated @RequestBody PartnerForm partnerForm) {
        Tenant tenant = authenticationFacade.getTenant(null);
        userService.linkTenantToPartner(tenant, partnerForm);
        return ok().build();
    }
}

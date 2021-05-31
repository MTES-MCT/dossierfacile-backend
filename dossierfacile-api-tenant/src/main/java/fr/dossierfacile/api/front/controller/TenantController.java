package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.form.SubscriptionTenantForm;
import fr.dossierfacile.api.front.mapper.PropertyOMapper;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.property.PropertyOModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.PropertyService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/tenant")
public class TenantController {

    private final AuthenticationFacade authenticationFacade;
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final PropertyService propertyService;
    private final PropertyOMapper propertyMapper;

    @GetMapping("/profile")
    public ResponseEntity<TenantModel> profile() {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        return ok(tenantMapper.toTenantModel(tenant));
    }

    @PostMapping("/subscribe/{token}")
    public ResponseEntity<Void> subscribeTenant(@PathVariable("token") String propertyToken, @Validated @RequestBody SubscriptionTenantForm subscriptionTenantForm) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        tenantService.subscribeTenant(propertyToken, subscriptionTenantForm, tenant);
        return ok().build();
    }

    @GetMapping("/property/{token}")
    public ResponseEntity<PropertyOModel> getOwner(@PathVariable("token") String propertyToken){
        Property property = propertyService.getPropertyByToken(propertyToken);
        PropertyOModel propertyModel = propertyMapper.toTenantModel(property);
        return ok(propertyModel);
    }
}

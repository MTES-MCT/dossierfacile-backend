package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLog;
import fr.dossierfacile.api.front.form.PartnerForm;
import fr.dossierfacile.api.front.mapper.PropertyOMapper;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.property.PropertyOModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.tenant.FranceConnectTaxForm;
import fr.dossierfacile.api.front.register.form.tenant.UrlForm;
import fr.dossierfacile.api.front.register.tenant.DocumentTax;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.PropertyService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
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

import static org.springframework.http.ResponseEntity.badRequest;
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

    private final DocumentTax documentTaxService;

    @GetMapping(value = "/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> profile() {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        tenantService.updateLastLoginDateAndResetWarnings(tenant);
        return ok(tenantMapper.toTenantModel(tenant));
    }

    @GetMapping(value = "/property/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PropertyOModel> getInfoOfPropertyAndOwner(@PathVariable("token") String propertyToken) {
        Property property = propertyService.getPropertyByToken(propertyToken);
        return ok(propertyMapper.toPropertyModel(property));
    }

    @DeleteMapping("/deleteCoTenant/{id}")
    public ResponseEntity<Void> deleteCoTenant(@PathVariable Long id) {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        return (userService.deleteCoTenant(tenant, id) ? ok() : status(HttpStatus.FORBIDDEN)).build();
    }

    @MethodLog
    @PostMapping(value = "/linkTenantToPartner", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> linkTenantToPartner(@Validated @RequestBody PartnerForm partnerForm) {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        userService.linkTenantToPartner(tenant, partnerForm.getSource(), partnerForm.getInternalPartnerId());
        return ok().build();
    }

    @PostMapping(value = "/allowTax/{allowTax}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> setAllowTax(@PathVariable("allowTax") String allowTax, @RequestBody FranceConnectTaxForm franceConnectTaxForm) {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        Boolean allow = BooleanUtils.toBooleanObject(allowTax, "allow", "disallow", "");
        documentTaxService.updateAutomaticTaxVerificationConsent(tenant, allow);
        if (allow && tenant.getFranceConnect()) {
            userService.checkDGFIPApi(tenant, franceConnectTaxForm);
        }
        TenantModel tenantModel = tenantMapper.toTenantModel(tenant);
        return ok(tenantModel);
    }

    @PostMapping(value = "/linkFranceConnect", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> linkFranceConnect(@RequestBody UrlForm urlDTO) {
        String currentUrl = urlDTO.getUrl();
        if (currentUrl == null) {
            return badRequest().build();
        }
        String link = authenticationFacade.getFranceConnectLink(currentUrl);
        return ok(link);
    }


}

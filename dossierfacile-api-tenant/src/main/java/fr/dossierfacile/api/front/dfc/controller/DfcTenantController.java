package fr.dossierfacile.api.front.dfc.controller;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/dfc/tenant")
public class DfcTenantController {

    private final AuthenticationFacade authenticationFacade;
    private final TenantMapper tenantMapper;
    private final UserService userService;

    @GetMapping(value = "/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectedTenantModel> profilePartner() {
        var tenant = authenticationFacade.getLoggedTenant();
        var partner = authenticationFacade.getKeycloakClientId();
        userService.linkTenantToPartner(tenant, partner);
        return ok(tenantMapper.toTenantModelDfc(tenant));
    }

    @RequestMapping(path="/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> logout() {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        userService.logout(tenant);
        return ok().build();
    }
}

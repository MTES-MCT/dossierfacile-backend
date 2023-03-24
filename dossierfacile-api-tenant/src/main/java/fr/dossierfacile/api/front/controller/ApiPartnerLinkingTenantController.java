package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLog;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api-partner-linking")
@MethodLog
public class ApiPartnerLinkingTenantController {

    private final AuthenticationFacade authenticationFacade;
    private final TenantMapper tenantMapper;
    private final UserService userService;

    @GetMapping(value = {"/{clientId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> linkTenantToPartner(@PathVariable String clientId) {
        Tenant tenantLogged = authenticationFacade.getLoggedTenant();
        userService.linkTenantToPartner(tenantLogged, clientId, null);
        return ok(tenantMapper.toTenantModel(tenantLogged));
    }
}

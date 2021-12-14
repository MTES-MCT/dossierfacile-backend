package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api-partner/tenant/{tenantId}/user")
@Validated
public class ApiPartnerUserController {

    private final UserService userService;
    private final AuthenticationFacade authenticationFacade;

    @DeleteMapping("/deleteAccount")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long tenantId) {
        Tenant tenant = authenticationFacade.getTenant(tenantId);
        userService.deleteAccount(tenant);
        return ok().build();
    }
}

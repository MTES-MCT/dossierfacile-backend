package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.model.tenant.EmailExistsModel;
import fr.dossierfacile.api.front.register.form.partner.EmailExistsForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import static org.springframework.http.ResponseEntity.ok;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api-partner")
@Validated
public class ApiPartnerUserController {

    private final UserService userService;
    private final AuthenticationFacade authenticationFacade;


    @PostMapping(value = "/emailexists", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmailExistsModel> emailExists(@Validated @RequestBody EmailExistsForm emailExistsForm) {
        return ok(userService.emailExists(emailExistsForm));
    }

    @DeleteMapping("/tenant/{tenantId}/user/deleteAccount")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long tenantId) {
        Tenant tenant = authenticationFacade.getTenant(tenantId);
        userService.deleteAccount(tenant);
        return ok().build();
    }
}

package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.form.EmailResetForm;
import fr.dossierfacile.api.front.form.PasswordForm;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/user")
@Validated
public class UserController {

    private final UserService userService;
    private final AuthenticationFacade authenticationFacade;

    @PostMapping("/forgotPassword")
    public ResponseEntity<Void> forgotPassword(@Validated @RequestBody EmailResetForm email) {
        userService.forgotPassword(email.getEmail());
        return ok().build();
    }

    @PostMapping("/createPassword/{token}")
    public ResponseEntity<TenantModel> createPassword(@PathVariable String token, @Validated @RequestBody PasswordForm password) {
        TenantModel tenantModel = userService.createPassword(token, password.getPassword());
        return ok(tenantModel);
    }

    @DeleteMapping("/deleteAccount")
    public ResponseEntity<Void> deleteAccount() {
        Tenant tenant = authenticationFacade.getTenant(null);
        userService.deleteAccount(tenant);
        return ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Tenant tenant = authenticationFacade.getTenant(null);
        userService.logout(tenant);
        return ok().build();
    }
}

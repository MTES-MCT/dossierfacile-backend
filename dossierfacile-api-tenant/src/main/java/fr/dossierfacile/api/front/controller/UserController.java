package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.form.EmailResetForm;
import fr.dossierfacile.api.front.form.PasswordForm;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
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

    @PostMapping(value = "/forgotPassword", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> forgotPassword(@Validated @RequestBody EmailResetForm email) {
        userService.forgotPassword(email.getEmail());
        return ok().build();
    }

    @PostMapping(value = "/createPassword", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Set a new Password to logged user")
    public ResponseEntity<TenantModel> createPassword(@Validated @RequestBody PasswordForm password) {
        return ok(userService.createPassword(authenticationFacade.getTenant(null), password.getPassword()));
    }

    @PostMapping(value = "/createPassword/{token}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> createPassword(@PathVariable String token, @Validated @RequestBody PasswordForm password) {
        return ok(userService.createPassword(token, password.getPassword()));
    }

    @DeleteMapping("/deleteAccount")
    public ResponseEntity<Void> deleteAccount() {
        Tenant tenant = authenticationFacade.getTenant(null);
        userService.deleteAccount(tenant);
        return ok().build();
    }

    @DeleteMapping("/franceConnect")
    @ApiOperation("Unlink account from FranceConnect")
    public ResponseEntity<Void> unlinkFranceConnect() {
        Tenant tenant = authenticationFacade.getTenant(null);
        userService.unlinkFranceConnect(tenant);
        return ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Tenant tenant = authenticationFacade.getTenant(null);
        userService.logout(tenant);
        return ok().build();
    }
}

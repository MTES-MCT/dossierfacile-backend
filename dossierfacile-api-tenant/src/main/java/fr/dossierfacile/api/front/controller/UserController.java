package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.form.EmailResetForm;
import fr.dossierfacile.api.front.form.PasswordForm;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/user")
@Validated
public class UserController {

    private final UserService userService;
    private final AuthenticationFacade authenticationFacade;

    @PostMapping(value = "/forgotPassword", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Request a password reset", notes = "Sends a password reset email to the user.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = ""),
            @ApiResponse(code = 400, message = "Invalid email format"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 403, message = "Forbidden: JWT token missing or invalid scope")
    })
    public ResponseEntity<Void> forgotPassword(@Validated @RequestBody EmailResetForm email) {
        userService.forgotPassword(email.getEmail());
        return ok().build();
    }

    @PostMapping(value = "/createPassword", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Set a new Password to logged user")
    public ResponseEntity<TenantModel> createPassword(@Validated @RequestBody PasswordForm password) {
        return ok(userService.createPassword(authenticationFacade.getLoggedTenant(), password.getPassword()));
    }

    @PostMapping(value = "/createPassword/{token}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> createPassword(@PathVariable String token, @Validated @RequestBody PasswordForm password) {
        return ok(userService.createPassword(token, password.getPassword()));
    }

    @DeleteMapping("/deleteAccount")
    public ResponseEntity<Void> deleteAccount() {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        userService.deleteAccount(tenant);
        return ok().build();
    }

    @DeleteMapping("/franceConnect")
    @ApiOperation("Unlink account from FranceConnect")
    public ResponseEntity<Void> unlinkFranceConnect() {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        userService.unlinkFranceConnect(tenant);
        return ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        userService.logout(authenticationFacade.getKeycloakUserId());
        return ok().build();
    }
}

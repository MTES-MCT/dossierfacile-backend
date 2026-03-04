package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.form.PasswordForm;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.logging.aop.SensitiveRequest;
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

    @PostMapping(value = "/createPassword/{token}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Set a new Password using token", notes = "Sets a new password using a provided token.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Password set successfully", response = TenantModel.class),
            @ApiResponse(code = 400, message = "Invalid token or password format"),
            @ApiResponse(code = 403, message = "Forbidden: JWT token missing or invalid scope")
    })
    @SensitiveRequest
    public ResponseEntity<TenantModel> createPassword(@PathVariable String token, @Validated @RequestBody PasswordForm password) {
        return ok(userService.createPassword(token, password.getPassword()));
    }


    @DeleteMapping("/deleteAccount")
    @ApiOperation(value = "Delete the current user account")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok"),
            @ApiResponse(code = 403, message = "Forbidden: User not verified or JWT token missing")
    })
    public ResponseEntity<Void> deleteAccount() {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        userService.deleteAccount(tenant);
        return ok().build();
    }
}

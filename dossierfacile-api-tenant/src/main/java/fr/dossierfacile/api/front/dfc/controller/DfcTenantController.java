package fr.dossierfacile.api.front.dfc.controller;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/dfc/tenant")
@Slf4j
public class DfcTenantController {

    private final AuthenticationFacade authenticationFacade;
    private final TenantMapper tenantMapper;
    private final TenantService tenantService;
    private final UserService userService;
    private final UserApiService userApiService;


    @ApiOperation(value = "Get tenant profile for partner", notes = "Retrieves the tenant profile associated with the authenticated partner.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Profile retrieved successfully", response = ConnectedTenantModel.class),
            @ApiResponse(code = 401, message = "Unauthorized: JWT token missing or invalid"),
            @ApiResponse(code = 403, message = "Forbidden: Insufficient scope")
    })
    @PreAuthorize("!isClient()")
    @GetMapping(value = "/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    // Todo : This logic need to be extracted to a service
    // Authentification facade method : getLoggedTenant() has the same logic
    public ResponseEntity<ConnectedTenantModel> profilePartner() {
        String partner = authenticationFacade.getKeycloakClientId();
        Tenant tenant = tenantService.findByKeycloakId(authenticationFacade.getKeycloakUserId());
        if (tenant == null) {
            log.error("User try to connect with not found keycloakId " + authenticationFacade.getUserEmail());
            tenant = tenantService.findByEmail(authenticationFacade.getUserEmail()).orElse(null);
        }

        if (tenant == null) {
            KeycloakUser kcUser = authenticationFacade.getKeycloakUser();
            tenant = tenantService.registerFromKeycloakUser(kcUser, partner, null);
        } else {
            userService.linkTenantToPartner(tenant, partner, null);
        }
        UserApi userApi = userApiService.findByName(authenticationFacade.getKeycloakClientId()).orElse(null);
        return ok(tenantMapper.toTenantModelDfc(tenant, userApi));
    }

    @ApiOperation(value = "Logout tenant", notes = "Logs out the authenticated tenant.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Logout successful"),
            @ApiResponse(code = 401, message = "Unauthorized: JWT token missing or invalid"),
            @ApiResponse(code = 403, message = "Forbidden: Insufficient scope")
    })
    @RequestMapping(path = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> logout() {

        userService.logout(authenticationFacade.getKeycloakUserId());
        return ok().build();
    }
}

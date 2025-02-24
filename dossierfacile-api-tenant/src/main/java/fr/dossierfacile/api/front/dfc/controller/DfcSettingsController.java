package fr.dossierfacile.api.front.dfc.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLogTime;
import fr.dossierfacile.api.front.mapper.PartnerSettingsMapper;
import fr.dossierfacile.api.front.model.dfc.PartnerSettings;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.UserApi;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/dfc/api/v1/settings")
@Slf4j
@MethodLogTime
public class DfcSettingsController {
    private final ClientAuthenticationFacade clientAuthenticationFacade;
    private UserApiService userApiService;
    private PartnerSettingsMapper partnerSettingsMapper;

    @ApiOperation(value = "Get current partner settings", notes = "Retrieves the current settings for the authenticated partner.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Settings retrieved successfully", response = PartnerSettings.class),
            @ApiResponse(code = 401, message = "Unauthorized: JWT token missing or invalid"),
            @ApiResponse(code = 403, message = "Forbidden: Insufficient scope")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PartnerSettings> get() {
        UserApi userApi = clientAuthenticationFacade.getClient();
        return ok(partnerSettingsMapper.toPartnerSettings(userApi));
    }

    @ApiOperation(value = "Update partner settings", notes = "Updates the settings for the authenticated partner.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Settings updated successfully", response = PartnerSettings.class),
            @ApiResponse(code = 401, message = "Unauthorized: JWT token missing or invalid"),
            @ApiResponse(code = 403, message = "Forbidden: Insufficient scope")
    })
    // Todo : Maybe add a @Valid annotation to validate the request body
    public ResponseEntity<PartnerSettings> update(@RequestBody PartnerSettings settings) {
        UserApi userApi = clientAuthenticationFacade.getClient();
        UserApi result = userApiService.update(userApi, settings);
        return ok(partnerSettingsMapper.toPartnerSettings(result));
    }
}
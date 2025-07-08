package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.form.ShareFileByMailForm;
import fr.dossierfacile.api.front.mapper.PropertyOMapper;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.property.PropertyOModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.form.tenant.UrlForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.PropertyService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.converter.AcquisitionData;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.service.interfaces.ProcessingCapacityService;
import io.swagger.annotations.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static org.springframework.http.ResponseEntity.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/tenant")
@Slf4j
public class TenantController {

    private final AuthenticationFacade authenticationFacade;
    private final TenantService tenantService;
    private final ProcessingCapacityService processingCapacityService;
    private final TenantMapper tenantMapper;
    private final PropertyService propertyService;
    private final PropertyOMapper propertyMapper;
    private final UserService userService;

    @GetMapping(value = "/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get tenant profile", notes = "Retrieves the profile of the logged-in tenant.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Profile retrieved successfully", response = TenantModel.class),
            @ApiResponse(code = 401, message = "Unauthorized: JWT token missing or invalid"),
            @ApiResponse(code = 403, message = "Forbidden: User not verified")
    })
    public ResponseEntity<TenantModel> profile(
            @ApiParam(value = "UTM campaign parameters", example = "campaign=utm_campaign&source=utm_source&medium=utm_medium")
            @RequestParam MultiValueMap<String, String> params
    ) {
        Tenant tenant = authenticationFacade.getLoggedTenant(AcquisitionData.from(params));
        tenantService.updateLastLoginDateAndResetWarnings(tenant);
        return ok(tenantMapper.toTenantModel(tenant, null));
    }


    @GetMapping(value = "/property/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get property and owner information", notes = "Retrieves information about a property and its owner based on the provided token.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Property information retrieved successfully", response = PropertyOModel.class),
            @ApiResponse(code = 401, message = "Unauthorized: JWT token missing or invalid"),
            @ApiResponse(code = 404, message = "Property not found")
    })
    public ResponseEntity<PropertyOModel> getInfoOfPropertyAndOwner(@PathVariable("token") String propertyToken) {
        Property property = propertyService.getPropertyByToken(propertyToken);
        return ok(propertyMapper.toPropertyModel(property));
    }

    @DeleteMapping("/deleteCoTenant/{id}")
    @ApiOperation(value = "Delete a co-tenant", notes = "Deletes a co-tenant based on the provided ID.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Co-tenant deleted successfully"),
            @ApiResponse(code = 403, message = "Forbidden: user not verified or co-tenant not found"),
            @ApiResponse(code = 401, message = "Unauthorized: JWT token missing or invalid")
    })
    public ResponseEntity<Void> deleteCoTenant(@PathVariable Long id) {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        return (userService.deleteCoTenant(tenant, id) ? ok() : status(HttpStatus.FORBIDDEN)).build();
    }

    @PostMapping(value = "/linkFranceConnect", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Link FranceConnect", notes = "Generates a link to FranceConnect based on the provided URL.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "FranceConnect link generated successfully"),
            @ApiResponse(code = 400, message = "Bad request: URL is missing or invalid"),
            @ApiResponse(code = 403, message = "Forbidden: JWT token missing or invalid")
    })
    public ResponseEntity<String> linkFranceConnect(@RequestBody UrlForm urlDTO) {
        // Todo : Could be replaced with @Valid annotation
        String currentUrl = urlDTO.getUrl();
        if (currentUrl == null) {
            return badRequest().build();
        }
        String link = authenticationFacade.getFranceConnectLink(currentUrl);
        return ok(link);
    }

    @PostMapping(value = "/sendFileByMail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Send File By Mail", notes = "Sends a file by email based on the provided email and share type.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "File sent successfully"),
            @ApiResponse(code = 400, message = "Bad request: email is invalid or limit reached"),
            @ApiResponse(code = 403, message = "Forbidden: JWT token missing or invalid"),
            @ApiResponse(code = 500, message = "Internal error: mail cannot be sent")
    })
    public ResponseEntity<String> sendFileByMail(@Valid @RequestBody ShareFileByMailForm shareFileByMailForm) {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        try {
            tenantService.sendFileByMail(tenant, shareFileByMailForm.getEmail(), shareFileByMailForm.getShareType());
            // Todo : inside the method sendFileByMail, there is an Internal error thrown and this exception is not caught by the controller
        } catch (Exception e) {
            return badRequest().build();
        }
        return ok("");
    }

    @PreAuthorize("hasPermissionOnTenant(#tenantId)")
    @GetMapping(value = "/{id}/expectedProcessingTime", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get Expected Processing Time", notes = "Retrieves the expected processing time for a tenant based on the provided ID.")
    @ApiResponses(value = {
            @ApiResponse(
                    code = 200,
                    message = "Expected processing time retrieved successfully",
                    response = LocalDateTime.class,
                    examples = @Example(value = {@ExampleProperty(mediaType = "application/json", value = "null")})),
            @ApiResponse(code = 401, message = "Unauthorized: JWT token missing or invalid"),
            @ApiResponse(code = 403, message = "Forbidden: Access denied"),
    })
    public ResponseEntity<LocalDateTime> expectedProcessingTime(@PathVariable("id") Long tenantId) {
        try {
            LocalDateTime expectedProcessingTime = processingCapacityService.getExpectedProcessingTime(tenantId);
            return ok(expectedProcessingTime);
        }
        catch (Exception e) {
            log.error("Error retrieving expected processing time for tenant with ID: {}", tenantId, e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/doNotArchive/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Do Not Archive", notes = "Prevents the archiving of a tenant based on the provided token.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Token processed successfully"),
            @ApiResponse(code = 401, message = "Unauthorized: JWT token missing or invalid"),
            @ApiResponse(code = 404, message = "Token not found")
    })
    public ResponseEntity<Void> doNotArchive(@PathVariable String token) {
        tenantService.doNotArchive(token);
        return ok().build();
    }
}

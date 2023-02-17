package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLog;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.SourceService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.model.TenantUpdate;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;


@RestController
@AllArgsConstructor
@RequestMapping("/api-partner/tenants")
@Validated
@MethodLog
public class ApiPartnerTenantsController {
    private final AuthenticationFacade authenticationFacade;
    private final TenantService tenantService;
    private final SourceService userApiService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TenantUpdate>> list(@RequestParam("lastUpdate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastUpdate) {
        Optional<UserApi> userApi = this.userApiService.findByName(authenticationFacade.getKeycloakClientId());

        return ok(tenantService.findTenantUpdateByLastUpdateAndPartner(lastUpdate, userApi.get()));
    }
}
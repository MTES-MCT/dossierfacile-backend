package fr.dossierfacile.api.front.dfc.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLogTime;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.ListMetadata;
import fr.dossierfacile.api.front.model.ResponseWrapper;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.model.TenantUpdate;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping(DfcTenantsController.PATH)
@Slf4j
@MethodLogTime
public class DfcTenantsController {
    static final String PATH = "/dfc/api/v1/tenants";
    private final ClientAuthenticationFacade clientAuthenticationFacade;
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final TenantPermissionsService tenantPermissionsService;

    @ApiOperation(value = "Gets a list of tenants associated",
            notes = "Result is ordered by last_update_date. Use 'after' parameter to define a starting point")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper<List<TenantUpdate>, ListMetadata>> list(@RequestParam(value = "after", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
                                                                                  @RequestParam(value = "limit", defaultValue = "1000") Long limit
    ) {
        UserApi userApi = clientAuthenticationFacade.getClient();
        List<TenantUpdate> result = tenantService.findTenantUpdateByLastUpdateAndPartner(after, userApi, limit);
        LocalDateTime nextTimeToken = (result.size() == 0) ? after : result.get(result.size() - 1).getLastUpdateDate();

        String nextLink = PATH + "?limit=" + limit + "&after=" + nextTimeToken;
        return ok(ResponseWrapper.<List<TenantUpdate>, ListMetadata>builder()
                .metadata(ListMetadata.builder()
                        .limit(limit)
                        .resultCount(result.size())
                        .nextLink(nextLink)
                        .build())
                .data(result)
                .build());
    }

    @ApiOperation(value = "Gets tenant by ID")
    @GetMapping(value = "/{tenantId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectedTenantModel> get(@PathVariable Long tenantId) {
        Tenant tenant = tenantService.findById(tenantId);
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!tenantPermissionsService.clientCanAccess(clientAuthenticationFacade.getKeycloakClientId(), tenant.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ok(tenantMapper.toTenantModelDfc(tenant));
    }
}

package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.aop.annotation.MethodLogTime;
import fr.dossierfacile.api.front.model.ListMetadata;
import fr.dossierfacile.api.front.model.ResponseWrapper;
import fr.dossierfacile.api.front.model.TenantSortType;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.model.TenantUpdate;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api-partner/tenant")
public class ApiPartnerTenantController {

    private final ClientAuthenticationFacade clientAuthenticationFacade;
    private final TenantService tenantService;

    @MethodLogTime
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper<List<TenantUpdate>, ListMetadata>> list(@RequestParam(value = "after", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
                                                                                  @RequestParam(value = "limit", defaultValue = "1000") Long limit,
                                                                                  @RequestParam(value = "orderBy", defaultValue = "LAST_UPDATE_DATE") TenantSortType orderBy,
                                                                                  @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted,
                                                                                  @RequestParam(value = "includeRevoked", defaultValue = "false") boolean includeRevoked
    ) {
        UserApi userApi = clientAuthenticationFacade.getClient();
        List<TenantUpdate> result;
        LocalDateTime nextTimeToken;
        switch (orderBy) {
            case CREATION_DATE -> {
                if (includeDeleted || includeRevoked) {
                    throw new IllegalArgumentException("includeDelete and includeRevoked are not available with creationDate order");
                }
                result = tenantService.findTenantUpdateByCreatedAndPartner(after, userApi, limit);
                nextTimeToken = result.isEmpty() ? after : result.get(result.size() - 1).getCreationDate();
            }
            case LAST_UPDATE_DATE -> {
                result = tenantService.findTenantUpdateByLastUpdateAndPartner(after, userApi, limit, includeDeleted, includeRevoked);
                nextTimeToken = result.isEmpty() ? after : result.get(result.size() - 1).getLastUpdateDate();
            }
            default -> throw new IllegalArgumentException();
        }

        String nextLink = UriComponentsBuilder.fromPath("/api-partner/tenant")
                .queryParam("limit", limit)
                .queryParam("orderBy", orderBy)
                .queryParam("after", nextTimeToken)
                .queryParam("includeDeleted", includeDeleted)
                .queryParam("includeRevoked", includeRevoked)
                .build().encode().toUriString();

        return ok(ResponseWrapper.<List<TenantUpdate>, ListMetadata>builder()
                .metadata(ListMetadata.builder()
                        .limit(limit)
                        .resultCount(result.size())
                        .nextLink(nextLink)
                        .build())
                .data(result)
                .build());

    }
}

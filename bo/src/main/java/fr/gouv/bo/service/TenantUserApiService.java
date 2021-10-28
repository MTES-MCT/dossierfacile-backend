package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.TenantUserApiKey;
import fr.dossierfacile.common.entity.UserApi;
import fr.gouv.bo.repository.TenantUserApiRepository;
import fr.gouv.bo.repository.UserApiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@RequiredArgsConstructor
@Service
@Slf4j
public class TenantUserApiService {

    private final TenantUserApiRepository tenantUserApiRepository;
    private final UserApiRepository userApiRepository;

    public void getTenantUserApi(Tenant tenant, Long id, String internalPartnerId) {
        TenantUserApi tenantUserApi = tenantUserApiRepository.findFirstByTenantAndUserApi(tenant, userApiRepository.getOne(id));
        UserApi userApi = userApiRepository.getOne(id);
        if (tenantUserApi == null) {
            tenantUserApi = TenantUserApi.builder()
                    .id(new TenantUserApiKey(tenant.getId(), userApi.getId()))
                    .tenant(tenant)
                    .userApi(userApi)
                    .allInternalPartnerId(
                            internalPartnerId != null && !internalPartnerId.isEmpty() ?
                                    Collections.singletonList(internalPartnerId) :
                                    Collections.emptyList()
                    )
                    .build();
            tenantUserApiRepository.save(tenantUserApi);
        } else {
            if (internalPartnerId != null && !internalPartnerId.isEmpty()) {
                if (tenantUserApi.getAllInternalPartnerId() == null) {
                    tenantUserApi.setAllInternalPartnerId(Collections.singletonList(internalPartnerId));
                } else if (!tenantUserApi.getAllInternalPartnerId().contains(internalPartnerId)) {
                    tenantUserApi.getAllInternalPartnerId().add(internalPartnerId);
                }
                tenantUserApiRepository.save(tenantUserApi);
            }
        }
    }

    public void save(TenantUserApi tenantUserApi) {
        tenantUserApiRepository.save(tenantUserApi);
    }
}

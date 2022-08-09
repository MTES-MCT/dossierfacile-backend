package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.TenantUserApiKey;
import fr.dossierfacile.common.entity.UserApi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantUserApiRepository extends JpaRepository<TenantUserApi, TenantUserApiKey> {
    Optional<TenantUserApi> findFirstByTenantAndUserApi(Tenant tenant, UserApi userApi);

    Optional<TenantUserApi> findFirstByTenantIdAndUserApiName(Long tenantId, String userApiName);
}

package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.TenantUserApiKey;
import fr.dossierfacile.common.entity.UserApi;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserApiRepository extends JpaRepository<TenantUserApi, TenantUserApiKey> {
    TenantUserApi findFirstByTenantAndUserApi(Tenant tenant, UserApi userApi);
}

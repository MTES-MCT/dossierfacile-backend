package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.TenantUserApiKey;
import fr.dossierfacile.common.entity.UserApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantUserApiRepository extends JpaRepository<TenantUserApi, TenantUserApiKey> {
    Optional<TenantUserApi> findFirstByTenantAndUserApi(Tenant tenant, UserApi userApi);

    Optional<TenantUserApi> findFirstByTenantIdAndUserApiName(Long tenantId, String userApiName);

    Optional<TenantUserApi> findFirstByUserApiAndTenantIn(UserApi partner, List<Tenant> tenants);

    @Query(value = """
            SELECT count(*) > 0
            FROM tenant_userapi
            INNER JOIN user_api ON user_api.id = tenant_userapi.userapi_id
            WHERE tenant_userapi.tenant_id = :tenantId AND user_api.name = :userApiName 
            """, nativeQuery = true)
    boolean existsByUserApiNameAndTenantId(@Param("userApiName") String userApiName, @Param("tenantId") Long tenantId);
}

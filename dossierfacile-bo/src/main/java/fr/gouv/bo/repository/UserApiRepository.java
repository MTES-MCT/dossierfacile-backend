package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TypeUserApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserApiRepository extends JpaRepository<UserApi, Long> {

    UserApi findOneByName(String source);

    List<UserApi> findAllByTypeUserApi(TypeUserApi light);

    UserApi findOneById(Long id);

    @Query(value = "select name FROM user_api join tenant_userapi " +
            "on user_api.id = tenant_userapi.userapi_id " +
            "WHERE tenant_userapi.tenant_id = :tenantId", nativeQuery = true)
    List<String> getNamesOfPartnerByTenantId(@Param("tenantId") Long tenantId);
}


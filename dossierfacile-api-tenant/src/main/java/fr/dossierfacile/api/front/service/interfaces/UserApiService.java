package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.dfc.PartnerSettings;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;

import java.util.List;
import java.util.Optional;

public interface UserApiService {
    UserApi findById(Long id);
    Optional<UserApi> findByName(String partner);
    boolean anyTenantIsLinked(UserApi partner, List<Tenant> tenants);
    UserApi update(UserApi userApi, PartnerSettings settings);
}

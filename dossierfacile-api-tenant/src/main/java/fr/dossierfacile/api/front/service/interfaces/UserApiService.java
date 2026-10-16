package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.dfc.PartnerSettings;
import fr.dossierfacile.common.entity.UserApi;

import java.util.Optional;

public interface UserApiService {
    UserApi findById(Long id);
    Optional<UserApi> findByName(String partner);
    UserApi update(UserApi userApi, PartnerSettings settings);
}

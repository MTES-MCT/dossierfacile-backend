package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.UserApi;

public interface UserApiService {

    UserApi findById(Long id);

}

package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.UserApi;

import java.util.Optional;

public interface SourceService {

    /** @deprecated use UserApiService */
    @Deprecated
    Optional<UserApi> findByName(String partner);
}

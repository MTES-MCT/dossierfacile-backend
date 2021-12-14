package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.UserApi;

import java.util.Optional;

public interface SourceService {
    UserApi findOrCreate(String name);

    Optional<UserApi> findByName(String partner);
}

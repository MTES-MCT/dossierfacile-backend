package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.UserApi;

public interface SourceService {
    UserApi findOrCreate(String name);
}

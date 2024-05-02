package fr.dossierfacile.api.front.mapper.mail;

import fr.dossierfacile.api.front.dto.UserApiDto;
import fr.dossierfacile.common.entity.TenantUserApi;
import fr.dossierfacile.common.entity.UserApi;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface UserApiMapperForMail {
    UserApiDto toDto(UserApi tenant);

    default UserApiDto toDto(TenantUserApi tenantUserApi) {
        return this.toDto(tenantUserApi.getUserApi());
    }
}
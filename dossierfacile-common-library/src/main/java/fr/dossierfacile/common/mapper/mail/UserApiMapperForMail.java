package fr.dossierfacile.common.mapper.mail;

import fr.dossierfacile.common.dto.mail.UserApiDto;
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
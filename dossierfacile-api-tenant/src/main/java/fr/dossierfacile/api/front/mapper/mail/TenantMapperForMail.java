package fr.dossierfacile.api.front.mapper.mail;

import fr.dossierfacile.api.front.dto.TenantDto;
import fr.dossierfacile.api.front.dto.UserApiDto;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantUserApi;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class TenantMapperForMail {

    @Autowired
    UserApiMapperForMail userApiMapperForMail;

    @Mapping(source = "tenant.tenantsUserApi", target = "userApis")
    public abstract TenantDto toDto(Tenant tenant);

    public UserApiDto toDto(TenantUserApi userApi) {
        return userApiMapperForMail.toDto(userApi);
    }
}

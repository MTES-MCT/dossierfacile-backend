package fr.dossierfacile.common.mapper.mail;

import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.dto.mail.UserApiDto;
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

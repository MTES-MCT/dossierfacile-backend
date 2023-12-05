package fr.dossierfacile.api.front.mapper;

import fr.dossierfacile.api.front.model.tenant.PartnerAccessModel;
import fr.dossierfacile.common.entity.TenantUserApi;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Mapper(componentModel = "spring")
public interface PartnerAccessMapper {

    List<PartnerAccessModel> toModel(List<TenantUserApi> message);

    @Mapping(source = "userApi.id", target = "id")
    @Mapping(source = "userApi.name2", target = "name")
    @Mapping(source = "userApi.logoUrl", target = "logoUrl")
    PartnerAccessModel toModel(TenantUserApi tenantUserApi);

}

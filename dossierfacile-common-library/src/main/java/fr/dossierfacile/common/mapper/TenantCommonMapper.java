package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.TenantModel;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class TenantCommonMapper {
    public abstract TenantModel toTenantModel(Tenant tenant);
}

package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.model.apartment_sharing.TenantModel;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface ApplicationBasicMapper extends ApartmentSharingMapper {

    ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing);

    ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing, @Context UserApi userApi);

    @Mapping(target = "allowCheckTax", ignore = true)
    @Mapping(target = "franceConnect", ignore = true)
    @Mapping(target = "guarantors", ignore = true)
    @Mapping(target = "documents", ignore = true)
    TenantModel toTenantModel(Tenant tenant);

}

package fr.gouv.bo.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.bo.model.owner.PropertyModel;
import fr.gouv.bo.model.tenant.ApartmentSharingModel;
import fr.gouv.bo.model.tenant.CoTenantModel;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Mapper(componentModel = "spring")
public interface PropertyMapper {

    PropertyModel toPropertyModel(Property property);

    default List<ApartmentSharingModel> toApartmentSharingModelList(List<PropertyApartmentSharing> pasList) {
        return pasList.stream().map(pas -> toApartmentSharingModel(pas.getApartmentSharing())).collect(Collectors.toList());
    }

    ApartmentSharingModel toApartmentSharingModel(ApartmentSharing apartmentSharing);

    CoTenantModel toCoTenantModel(Tenant tenant);

}

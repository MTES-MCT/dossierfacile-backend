package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.api.dossierfacileapiowner.property.ApartmentSharingModel;
import fr.dossierfacile.api.dossierfacileapiowner.property.LightPropertyModel;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.enums.TenantFileStatus;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class OwnerMapper {

    @Mapping(target = "id", ignore = true)
    public abstract OwnerModel toOwnerModel(Owner owner);

    @Mapping( target="totalSalary", expression="java(apartmentSharing.totalSalary())" )
    @Mapping( target="totalGuarantorSalary", expression="java(apartmentSharing.totalGuarantorSalary())" )
    public abstract ApartmentSharingModel apartmentSharingToApartmentSharingModel(ApartmentSharing apartmentSharing);

    @Mapping( source="property", target="propertyApartmentSharingCount", qualifiedByName="validApplicationsCount" )
    @Mapping( source="dpeDate", target="dpeDate", dateFormat="yyyy-MM-dd" )
    public abstract LightPropertyModel toLightPropertyModel(Property property);

    @Named("validApplicationsCount")
    public static int getValidApplicationsCount(Property property) {
        long count = property.getPropertiesApartmentSharing().stream()
            .filter(p -> !p.getApartmentSharing().getStatus().equals(TenantFileStatus.ARCHIVED))
            .count();
        return (int) count;
    }
}

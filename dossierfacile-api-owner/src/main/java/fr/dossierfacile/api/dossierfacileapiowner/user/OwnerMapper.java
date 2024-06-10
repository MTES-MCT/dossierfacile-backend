package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.api.dossierfacileapiowner.property.ApartmentSharingModel;
import fr.dossierfacile.api.dossierfacileapiowner.property.LightPropertyModel;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class OwnerMapper {

    @Mapping(target = "id", ignore = true)
    public abstract OwnerModel toOwnerModel(Owner owner);

    @Mapping( target="totalSalary", expression="java(apartmentSharing.totalSalary())" )
    @Mapping( target="totalGuarantorSalary", expression="java(apartmentSharing.totalGuarantorSalary())" )
    public abstract ApartmentSharingModel apartmentSharingToApartmentSharingModel(ApartmentSharing apartmentSharing);

    @Mapping( target="propertyApartmentSharingCount", expression="java(property.getPropertiesApartmentSharing().size())" )
    @Mapping( source="dpeDate", target="dpeDate", dateFormat="yyyy-MM-dd")
    public abstract LightPropertyModel toLightPropertyModel(Property property);
}

package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class PropertyMapper {

    public abstract PropertyModel toPropertyModel(Property property);

    @Mapping( target="totalSalary", expression="java(apartmentSharing.totalSalary())" )
    @Mapping( target="totalGuarantorSalary", expression="java(apartmentSharing.totalGuarantorSalary())" )
    public abstract ApartmentSharingModel apartmentSharingToApartmentSharingModel(ApartmentSharing apartmentSharing);
}

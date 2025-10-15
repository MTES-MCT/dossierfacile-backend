package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Property;

import java.util.List;
import java.util.Optional;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class PropertyMapper {

    public PropertyModel toPropertyModel(Property property) {
        return map(property, property);
    }

    abstract PropertyModel map(Property property, @Context Property propertyContext);

    @Mapping(target = "totalSalary", expression = "java(apartmentSharing.totalSalary())")
    @Mapping(target = "totalGuarantorSalary", expression = "java(apartmentSharing.totalGuarantorSalary())")
    @Mapping(target = "token", expression = "java(getToken(apartmentSharing, property))")
    public abstract ApartmentSharingModel apartmentSharingToApartmentSharingModel(ApartmentSharing apartmentSharing, @Context Property property);

    String getToken(ApartmentSharing apartmentSharing, Property property) {
        List<ApartmentSharingLink> links = property.getApartmentSharingLinks();
        Optional<ApartmentSharingLink> link = links.stream().filter(l -> l.getApartmentSharing().equals(apartmentSharing)).findFirst();
        return link.isPresent() ? link.get().getToken().toString() : null;
    }
}

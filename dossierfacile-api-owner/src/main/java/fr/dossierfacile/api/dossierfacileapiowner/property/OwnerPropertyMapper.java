package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;

import java.util.Optional;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class OwnerPropertyMapper {

    @Mapping( source="dpeDate", target="dpeDate", dateFormat="yyyy-MM-dd")
    public abstract PropertyModel toPropertyModel(Property property);

    @Mapping( target="ownerName", expression="java(property.getOwner().getFullName())" )
    @Mapping( source="dpeDate", target="dpeDate", dateFormat="yyyy-MM-dd")
    public abstract LightPropertyModel toLightPropertyModel(Property property);

    @AfterMapping
    void modificationsAfterMapping(@MappingTarget PropertyModel.PropertyModelBuilder propertyModelBuilder, Property property) {
        PropertyModel propertyModel = propertyModelBuilder.build();
        for (PropertyApartmentSharingModel propertyApartmentSharing : propertyModel.getPropertiesApartmentSharing()) {
            Optional<PropertyApartmentSharing> aptSharing = property.getPropertiesApartmentSharing().stream()
                .filter(p -> p.getId().equals(propertyApartmentSharing.getId()))
                .findFirst();
            if (aptSharing.isPresent()) {
                Optional<ApartmentSharingLink> aptLink = aptSharing.get().getApartmentSharing().getApartmentSharingLinks().stream()
                    .filter(link -> ApartmentSharingLinkType.OWNER.equals(link.getLinkType()) && link.getPropertyId() == property.getId())
                    .findFirst();
                String token = aptLink.isPresent() ? aptLink.get().getToken().toString() : "";
                propertyApartmentSharing.getApartmentSharing().setToken(token);
            }
        }
    }
}

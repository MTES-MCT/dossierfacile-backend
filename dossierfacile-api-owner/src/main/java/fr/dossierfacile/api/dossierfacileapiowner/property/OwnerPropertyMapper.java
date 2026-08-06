package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.PartnerVisibleStatus;

import java.util.List;
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
        List<PropertyApartmentSharingModel> propertiesApartmentSharing = propertyModel.getPropertiesApartmentSharing();
        if (propertiesApartmentSharing == null) {
            return;
        }
        for (PropertyApartmentSharingModel propertyApartmentSharing : propertiesApartmentSharing) {
            Optional<PropertyApartmentSharing> aptSharing = property.getPropertiesApartmentSharing().stream()
                .filter(p -> p.getId().equals(propertyApartmentSharing.getId()))
                .findFirst();
            if (aptSharing.isPresent()) {
                Optional<ApartmentSharingLink> aptLink = aptSharing.get().getApartmentSharing().getApartmentSharingLinks().stream()
                        .filter(link -> ApartmentSharingLinkType.OWNER.equals(link.getLinkType()) && link.getPropertyId().equals(property.getId()))
                        .filter(ApartmentSharingLink::isActive)
                    .findFirst();
                String token = aptLink.isPresent() ? aptLink.get().getToken().toString() : "";
                propertyApartmentSharing.getApartmentSharing().setToken(token);
            }
        }
    }

    // Defensive safety net: the COMPLETED status must never be exposed to owners
    protected String toOwnerVisibleStatus(TenantFileStatus status) {
        if (status == null) {
            return null;
        }
        return PartnerVisibleStatus.mask(status, getClass().getSimpleName()).name();
    }

    protected TenantFileStatus toOwnerVisibleTenantStatus(TenantFileStatus status) {
        return PartnerVisibleStatus.mask(status, getClass().getSimpleName());
    }
}

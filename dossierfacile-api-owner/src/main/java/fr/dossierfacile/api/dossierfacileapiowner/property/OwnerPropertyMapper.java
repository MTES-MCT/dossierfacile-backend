package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class OwnerPropertyMapper {

    public abstract PropertyModel toPropertyModel(Property property);

    @Mapping( target="ownerName", expression="java(property.getOwner().getFullName())" )
    public abstract LightPropertyModel toLightPropertyModel(Property property);

}

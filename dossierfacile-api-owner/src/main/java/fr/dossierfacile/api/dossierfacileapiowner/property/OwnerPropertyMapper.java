package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class OwnerPropertyMapper {

    @Mapping( source="dpeDate", target="dpeDate", dateFormat="yyyy-MM-dd")
    public abstract PropertyModel toPropertyModel(Property property);

    @Mapping( target="ownerName", expression="java(property.getOwner().getFullName())" )
    @Mapping( source="dpeDate", target="dpeDate", dateFormat="yyyy-MM-dd")
    public abstract LightPropertyModel toLightPropertyModel(Property property);

}

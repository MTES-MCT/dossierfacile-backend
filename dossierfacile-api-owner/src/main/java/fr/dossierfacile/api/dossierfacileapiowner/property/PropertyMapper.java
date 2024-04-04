package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class PropertyMapper {

    public abstract PropertyModel toPropertyModel(Property property);

}

package fr.gouv.bo.mapper;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import fr.gouv.bo.model.owner.OwnerModel;
import fr.gouv.bo.model.owner.PropertyModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface OwnerMapper {
    OwnerModel toOwnerModel(Owner owner);

    @Mapping(target = "propertiesApartmentSharing", ignore = true)
    PropertyModel toPropertyModel(Property property);

}

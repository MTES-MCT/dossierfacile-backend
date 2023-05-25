package fr.gouv.bo.mapper;

import fr.dossierfacile.common.entity.Owner;
import fr.gouv.bo.model.owner.OwnerModel;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class OwnerMapper {

    public abstract OwnerModel toOwnerModel(Owner owner);

}

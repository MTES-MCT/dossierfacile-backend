package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.common.entity.Owner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class OwnerMapper {

    @Mapping(target = "id", ignore = true)
    public abstract OwnerModel toOwnerModel(Owner owner);

}

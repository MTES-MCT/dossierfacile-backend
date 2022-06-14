package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.api.dossierfacileapiowner.property.ApartmentSharingModel;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Owner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class OwnerMapper {

    @Mapping(target = "id", ignore = true)
    public abstract OwnerModel toOwnerModel(Owner owner);

    @Mapping( target="totalSalary", expression="java(apartmentSharing.totalSalary())" )
    @Mapping( target="totalGuarantorSalary", expression="java(apartmentSharing.totalGuarantorSalary())" )
    public abstract ApartmentSharingModel apartmentSharingToApartmentSharingModel(ApartmentSharing apartmentSharing);

}

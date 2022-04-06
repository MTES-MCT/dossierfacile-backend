package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.ApartmentSharing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ApartmentSharingModelMapper {
    @Mapping( target="totalSalary", expression="java(apartmentSharing.totalSalary())" )
    ApartmentSharingModel apartmentSharingToApartmentSharingModel(ApartmentSharing apartmentSharing);
}
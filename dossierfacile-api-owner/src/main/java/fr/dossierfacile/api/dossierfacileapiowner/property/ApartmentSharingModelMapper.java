package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.PartnerVisibleStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ApartmentSharingModelMapper {
    @Mapping( target="totalSalary", expression="java(apartmentSharing.totalSalary())" )
    @Mapping( target="totalGuarantorSalary", expression="java(apartmentSharing.totalGuarantorSalary())" )
    ApartmentSharingModel apartmentSharingToApartmentSharingModel(ApartmentSharing apartmentSharing);

    // Defensive safety net: the COMPLETED status must never be exposed to owners
    default String toOwnerVisibleStatus(TenantFileStatus status) {
        if (status == null) {
            return null;
        }
        return PartnerVisibleStatus.mask(status, getClass().getSimpleName()).name();
    }

    default TenantFileStatus toOwnerVisibleTenantStatus(TenantFileStatus status) {
        return PartnerVisibleStatus.mask(status, getClass().getSimpleName());
    }
}
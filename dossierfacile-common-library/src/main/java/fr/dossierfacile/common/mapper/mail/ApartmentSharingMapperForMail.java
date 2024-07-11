package fr.dossierfacile.common.mapper.mail;

import fr.dossierfacile.common.dto.mail.ApartmentSharingDto;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class ApartmentSharingMapperForMail {
    @Autowired
    TenantMapperForMail tenantMapperForMail;

    public abstract ApartmentSharingDto toDto(ApartmentSharing apartmentSharing);

    public TenantDto toDto(Tenant tenant) {
        return tenantMapperForMail.toDto(tenant);
    }
}

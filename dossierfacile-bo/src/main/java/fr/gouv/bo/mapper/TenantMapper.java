package fr.gouv.bo.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.bo.model.tenant.ApartmentSharingModel;
import fr.gouv.bo.model.tenant.FileModel;
import fr.gouv.bo.model.tenant.TenantModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class TenantMapper {

    public abstract TenantModel toTenantModel(Tenant tenant);

    @Mapping(target = "path", ignore = true)
    public abstract FileModel toFileModel(File file);

    @Mapping(target = "tokenPublic", ignore = true)
    @Mapping(target = "dossierUrl", ignore = true)
    @Mapping(target = "dossierPdfUrl", ignore = true)
    public abstract ApartmentSharingModel toApartmentSharingModel(ApartmentSharing apartmentSharing);

}

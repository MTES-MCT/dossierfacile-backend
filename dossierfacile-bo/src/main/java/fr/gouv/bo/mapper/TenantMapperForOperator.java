package fr.gouv.bo.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.bo.model.tenant.ApartmentSharingModel;

import fr.gouv.bo.model.tenant.DocumentModel;
import fr.gouv.bo.model.tenant.FileModel;
import fr.gouv.bo.model.tenant.TenantModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class TenantMapperForOperator {

    public abstract TenantModel toTenantModel(Tenant tenant);

    @Mapping(target = "name", ignore = true)
    public abstract DocumentModel toDocumentModel(Document document);

    @Mapping(target = "originalName", ignore = true)
    @Mapping(target = "path", expression = "java((file.getStorageFile() != null )? file.getStorageFile().getPath() : null)")
    @Mapping(target = "contentType", expression = "java((file.getStorageFile() != null )? file.getStorageFile().getContentType() : null)")
    public abstract FileModel toFileModel(File file);


    @Mapping(target = "token", ignore = true)
    @Mapping(target = "tokenPublic", ignore = true)
    @Mapping(target = "dossierPdfUrl", ignore = true)
    public abstract ApartmentSharingModel toApartmentSharingModel(ApartmentSharing apartmentSharing);

}

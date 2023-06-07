package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import fr.dossierfacile.common.model.apartment_sharing.TenantModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class TenantCommonMapper {
    public abstract TenantModel toTenantModel(Tenant tenant);
    @Mapping(target = "name", expression = "java((document.getWatermarkFile() != null )? document.getWatermarkFile().getName() : null)")
    public abstract DocumentModel toDocumentModel(Document document);
}

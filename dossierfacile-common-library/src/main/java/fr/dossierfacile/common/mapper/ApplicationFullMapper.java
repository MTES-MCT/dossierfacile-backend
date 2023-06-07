package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import fr.dossierfacile.common.model.apartment_sharing.TenantModel;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class ApplicationFullMapper implements ApartmentSharingMapper {
    protected static final String PATH = "api/document/resource";

    @Value("${application.domain:default}")
    protected String domain;

    public abstract ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing);

    public abstract ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing, @Context UserApi userApi);

    @Mapping(target = "name", expression = "java((document.getWatermarkFile() != null )? domain + \"/\" + PATH + \"/\" + document.getName() : null)")
    public abstract DocumentModel toDocumentModel(Document document);

    @Mapping(target = "partnerLinked", expression = "java((userApi == null)? null : tenant.getTenantsUserApi() != null && tenant.getTenantsUserApi().stream().anyMatch( t -> t.getUserApi().getId() == userApi.getId()))")
    public abstract TenantModel toTenantModel(Tenant tenant, @Context UserApi userApi);

    @BeforeMapping
    void enrichModelWithDossierPdfUrl(ApartmentSharing apartmentSharing, @MappingTarget ApplicationModel.ApplicationModelBuilder applicationModelBuilder) {
        if (apartmentSharing.getStatus() == TenantFileStatus.VALIDATED) {
            applicationModelBuilder.dossierPdfUrl(domain + "/api/application/fullPdf/" + apartmentSharing.getToken());
        }
    }
}

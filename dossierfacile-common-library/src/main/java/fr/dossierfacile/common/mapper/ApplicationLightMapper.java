package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class ApplicationLightMapper implements ApartmentSharingMapper {

    public abstract ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing);

    @Mapping(target = "name", ignore = true)
    @MapDocumentCategories
    public abstract DocumentModel documentToDocumentModel(Document document, @Context UserApi userApi);


    public DocumentModel documentToDocumentModel(Document document) {
        return documentToDocumentModel(document, null);
    }
}

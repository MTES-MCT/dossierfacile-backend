package fr.dossierfacile.api.front.mapper;

import fr.dossierfacile.api.front.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.api.front.model.apartment_sharing.DocumentModel;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface ApplicationLightMapper {
    ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing);

    @Mapping(target = "name", ignore = true)
    DocumentModel documentToDocumentModel(Document document);
}

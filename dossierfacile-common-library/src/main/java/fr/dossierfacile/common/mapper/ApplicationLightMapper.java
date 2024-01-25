package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class ApplicationLightMapper implements ApartmentSharingMapper {

    protected SubCategoryMapper subCategoryMapper;

    @Autowired
    public void setSubCategoryMapper(SubCategoryMapper subCategoryMapper) {
        this.subCategoryMapper = subCategoryMapper;
    }

    public abstract ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing);

    @Mapping(target = "name",  ignore = true)
    @MapDocumentSubCategory
    @Mapping(target = "authenticityStatus", expression = "java(fr.dossierfacile.common.entity.AuthenticityStatus.isAuthentic(document))")
    public abstract DocumentModel documentToDocumentModel(Document document);

}

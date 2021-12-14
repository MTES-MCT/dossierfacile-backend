package fr.gouv.bo.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.gouv.bo.model.apartment_sharing.ApplicationModel;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

@Component
@Mapper(componentModel = "spring")
public abstract class ApplicationFullMapper {

    @Value("${application.domain}")
    private String domain;
    @Value("${application.file.path}")
    private String path;

    public abstract ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing);

    @BeforeMapping
    protected void enrichModelWithDossierPdfUrl(ApartmentSharing apartmentSharing, @MappingTarget ApplicationModel.ApplicationModelBuilder applicationModelBuilder) {
        applicationModelBuilder.dossierPdfUrl(domain + "/api/application/fullPdf/" + apartmentSharing.getToken());
    }

    @AfterMapping
    void addDomainAndPath(@MappingTarget ApplicationModel.ApplicationModelBuilder applicationModelBuilder) {
        ApplicationModel applicationModel = applicationModelBuilder.build();
        Optional.ofNullable(applicationModel.getTenants())
                .orElse(new ArrayList<>())
                .forEach(tenantModel -> {
                    Optional.ofNullable(tenantModel.getDocuments())
                            .orElse(new ArrayList<>())
                            .forEach(documentModel -> documentModel.setName(domain + "/" + path + "/" + documentModel.getName()));
                    Optional.ofNullable(tenantModel.getGuarantors())
                            .orElse(new ArrayList<>())
                            .forEach(guarantorModel ->
                                    Optional.ofNullable(guarantorModel.getDocuments())
                                            .orElse(new ArrayList<>())
                                            .forEach(documentModel -> documentModel.setName(domain + "/" + path + "/" + documentModel.getName())));
                });
    }
}

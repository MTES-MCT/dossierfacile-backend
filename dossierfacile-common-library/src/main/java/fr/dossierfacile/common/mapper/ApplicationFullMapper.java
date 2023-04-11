package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
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
public abstract class ApplicationFullMapper implements ApartmentSharingMapper {
    private static final String PATH = "api/document/resource";

    @Value("${application.domain:default}")
    private String domain;

    public abstract ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing);

    @BeforeMapping
    void enrichModelWithDossierPdfUrl(ApartmentSharing apartmentSharing, @MappingTarget ApplicationModel.ApplicationModelBuilder applicationModelBuilder) {
        if (apartmentSharing.getStatus() == TenantFileStatus.VALIDATED) {
            applicationModelBuilder.dossierPdfUrl(domain + "/api/application/fullPdf/" + apartmentSharing.getToken());
        }
    }

    @AfterMapping
    void addDomainAndPath(@MappingTarget ApplicationModel.ApplicationModelBuilder applicationModelBuilder) {
        ApplicationModel applicationModel = applicationModelBuilder.build();
        Optional.ofNullable(applicationModel.getTenants())
                .orElse(new ArrayList<>())
                .forEach(tenantModel -> {
                    Optional.ofNullable(tenantModel.getDocuments())
                            .orElse(new ArrayList<>())
                            .forEach(documentModel -> documentModel.setName(domain + "/" + PATH + "/" + documentModel.getName()));
                    Optional.ofNullable(tenantModel.getGuarantors())
                            .orElse(new ArrayList<>())
                            .forEach(guarantorModel ->
                                    Optional.ofNullable(guarantorModel.getDocuments())
                                            .orElse(new ArrayList<>())
                                            .forEach(documentModel -> documentModel.setName(domain + "/" + PATH + "/" + documentModel.getName())));
                });
    }
}

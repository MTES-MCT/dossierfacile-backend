package fr.gouv.bo.mapper;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.gouv.bo.model.tenant.ApartmentSharingModel;
import fr.gouv.bo.model.tenant.TenantModel;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

@Component
@Mapper(componentModel = "spring")
public abstract class TenantMapper {
    @Value("${application.domain}")
    private String domain;

    public abstract TenantModel toTenantModel(Tenant tenant);

    @AfterMapping
    void modificationsAfterMapping(@MappingTarget TenantModel.TenantModelBuilder tenantModelBuilder) {
        TenantModel tenantModel = tenantModelBuilder.build();
        ApartmentSharingModel apartmentSharingModel = tenantModel.getApartmentSharing();
        if (apartmentSharingModel.getStatus() != TenantFileStatus.VALIDATED) {
            apartmentSharingModel.setToken(null);
            apartmentSharingModel.setTokenPublic(null);
        }
        if (apartmentSharingModel.getStatus() == TenantFileStatus.VALIDATED) {
            apartmentSharingModel.setDossierPdfUrl(domain + "/api/application/fullPdf/" + apartmentSharingModel.getToken());
        }

        Optional.ofNullable(tenantModel.getDocuments())
                .orElse(new ArrayList<>())
                .forEach(documentModel -> Optional.ofNullable(documentModel.getFiles())
                        .orElse(new ArrayList<>())
                        .forEach(fileModel -> fileModel.setPath(domain + "/file/" + fileModel.getId())
                        ));
        Optional.ofNullable(tenantModel.getGuarantors())
                .orElse(new ArrayList<>())
                .forEach(guarantorModel -> Optional.ofNullable(guarantorModel.getDocuments())
                        .orElse(new ArrayList<>())
                        .forEach(documentModel -> Optional.ofNullable(documentModel.getFiles())
                                .orElse(new ArrayList<>())
                                .forEach(fileModel -> fileModel.setPath(domain + "/file/" + fileModel.getId())
                                )));

    }
}

package fr.dossierfacile.api.front.mapper;

import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.model.tenant.ApartmentSharingModel;
import fr.dossierfacile.api.front.model.tenant.DocumentModel;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Mapper(componentModel = "spring")
public abstract class TenantMapper {
    @Value("${application.domain}")
    private String domain;
    @Value("${application.file.path}")
    private String path;

    public abstract TenantModel toTenantModel(Tenant tenant);

    @Mapping(target = "connectedTenantId", source = "id")
    public abstract ConnectedTenantModel toTenantModelDfc(Tenant tenant);

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
        var isDossierUser = SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_dossier"));
        var filePath = isDossierUser ? "/api/file/resource/" : "/api-partner/tenant/" + tenantModel.getId() + "/file/resource/";
        setDocumentAndFilesRoutes(tenantModel.getDocuments(), filePath);

        Optional.ofNullable(tenantModel.getGuarantors())
                .ifPresent(guarantorModels -> guarantorModels.forEach(guarantorModel -> setDocumentAndFilesRoutes(guarantorModel.getDocuments(), filePath)));
    }

    private void setDocumentAndFilesRoutes(List<DocumentModel> list, String filePath) {
        Optional.ofNullable(list)
                .ifPresent(documentModels -> documentModels.forEach(documentModel -> {
                    documentModel.setName(domain + "/" + path + "/" + documentModel.getName());
                    Optional.ofNullable(documentModel.getFiles())
                            .ifPresent(fileModels -> fileModels.forEach(fileModel -> fileModel.setPath(domain + filePath + fileModel.getId())));
                }));
    }

    @AfterMapping
    void modificationsAfterMapping(@MappingTarget ConnectedTenantModel.ConnectedTenantModelBuilder connectedTenantModelBuilder) {
        ConnectedTenantModel connectedTenantModel = connectedTenantModelBuilder.build();
        fr.dossierfacile.api.front.model.dfc.apartment_sharing.ApartmentSharingModel apartmentSharingModel = connectedTenantModel.getApartmentSharing();
        if (apartmentSharingModel.getStatus() != TenantFileStatus.VALIDATED) {
            apartmentSharingModel.setToken(null);
            apartmentSharingModel.setTokenPublic(null);
        }
        if (apartmentSharingModel.getStatus() == TenantFileStatus.VALIDATED) {
            apartmentSharingModel.setDossierPdfUrl(domain + "/api/application/fullPdf/" + apartmentSharingModel.getToken());
        }
        connectedTenantModel.getApartmentSharing().getTenants().forEach(tenantModel -> setDocumentRoutesForDFC(tenantModel.getDocuments()));
        connectedTenantModel.getApartmentSharing().getTenants().forEach(tenantModel ->
                Optional.ofNullable(tenantModel.getGuarantors()).ifPresent(guarantorModels ->
                        guarantorModels.forEach(guarantorModel -> setDocumentRoutesForDFC(guarantorModel.getDocuments()))));
    }

    private void setDocumentRoutesForDFC(List<fr.dossierfacile.api.front.model.dfc.apartment_sharing.DocumentModel> list) {
        Optional.ofNullable(list)
                .ifPresent(documentModels -> documentModels.forEach(documentModel -> documentModel.setName(domain + "/" + path + "/" + documentModel.getName())));
    }
}

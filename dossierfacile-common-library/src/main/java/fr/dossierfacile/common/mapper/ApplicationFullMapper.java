package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import fr.dossierfacile.common.model.apartment_sharing.GuarantorModel;
import fr.dossierfacile.common.model.apartment_sharing.TenantModel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class ApplicationFullMapper implements ApartmentSharingMapper {
    protected static final String DOCUMENT_DIRECT_PATH = "api/document/resource";
    protected static final String DOCUMENT_LINK_PATH = "api/application/links";
    protected static final String DOSSIER_PDF_PATH = "api/application/fullPdf";
    protected static final String DOSSIER_PATH = "file";

    @Value("${application.base.url:default}")
    protected String applicationBaseUrl;

    @Value("${tenant.base.url:default}")
    protected String tenantBaseUrl;

    abstract ApplicationModel mapApplicationModel(ApartmentSharing apartmentSharing, @Context UserApi userApi);

    @Override
    public ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing, UserApi userApi) {
        ApplicationModel model = mapApplicationModel(apartmentSharing, userApi);
        resolvePartnerToken(apartmentSharing, userApi).ifPresentOrElse(
            token -> {
                model.setDossierPdfUrl(applicationBaseUrl + "/" + DOSSIER_PDF_PATH + "/" + token);
                model.setDossierUrl(tenantBaseUrl + "/" + DOSSIER_PATH + "/" + token);
                buildDocumentUrls(model, applicationBaseUrl + "/" + DOCUMENT_LINK_PATH + "/" + token + "/documents/");
            },
            () -> buildDocumentUrls(model, applicationBaseUrl + "/" + DOCUMENT_DIRECT_PATH + "/")
        );
        return model;
    }

    public ApplicationModel toApplicationModel(ApartmentSharing apartmentSharing) {
        return toApplicationModel(apartmentSharing, null);
    }

    public ApplicationModel toApplicationModelWithToken(ApartmentSharing apartmentSharing, UUID token) {
        ApplicationModel model = mapApplicationModel(apartmentSharing, null);
        model.setDossierPdfUrl(applicationBaseUrl + "/" + DOSSIER_PDF_PATH + "/" + token);
        model.setDossierUrl(tenantBaseUrl + "/" + DOSSIER_PATH + "/" + token);
        buildDocumentUrls(model, applicationBaseUrl + "/" + DOCUMENT_LINK_PATH + "/" + token + "/documents/");
        return model;
    }

    @Mapping(target = "name", expression = "java((document.getWatermarkFile() != null) ? document.getName() : null)")
    @Mapping(target = "authenticityStatus", expression = "java(fr.dossierfacile.common.entity.AuthenticityStatus.isAuthentic(document))")
    @MapDocumentCategories
    public abstract DocumentModel toDocumentModel(Document document, @Context UserApi userApi);

    @Mapping(target = "partnerLinked", expression = "java((userApi == null)? null : tenant.getTenantsUserApi() != null && tenant.getTenantsUserApi().stream().anyMatch( t -> t.getUserApi().getId() == userApi.getId()))")
    public abstract TenantModel toTenantModel(Tenant tenant, @Context UserApi userApi);

    private Optional<String> resolvePartnerToken(ApartmentSharing apartmentSharing, UserApi userApi) {
        if (apartmentSharing.getStatus() != TenantFileStatus.VALIDATED || userApi == null) {
            return Optional.empty();
        }
        return apartmentSharing.getApartmentSharingLinks().stream()
            .filter(l -> l.getLinkType() == ApartmentSharingLinkType.PARTNER
                && Objects.equals(l.getPartnerId(), userApi.getId())
                && l.isFullData())
            .findFirst()
            .map(l -> l.getToken().toString());
    }

    private void buildDocumentUrls(ApplicationModel model, String urlPrefix) {
        if (model.getTenants() == null) {
            return;
        }
        for (TenantModel tenant : model.getTenants()) {
            prefixDocumentUrls(tenant.getDocuments(), urlPrefix);
            if (tenant.getGuarantors() != null) {
                for (GuarantorModel guarantor : tenant.getGuarantors()) {
                    prefixDocumentUrls(guarantor.getDocuments(), urlPrefix);
                }
            }
        }
    }

    private void prefixDocumentUrls(List<DocumentModel> documents, String urlPrefix) {
        if (documents == null) {
            return;
        }
        for (DocumentModel doc : documents) {
            if (doc.getName() != null) {
                doc.setName(urlPrefix + doc.getName());
            }
        }
    }
}

package fr.dossierfacile.api.front.mapper;

import fr.dossierfacile.api.front.model.BaseApartmentSharingModel;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.model.tenant.*;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.mapper.MapDocumentCategories;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Component
@Mapper(componentModel = "spring")
public abstract class TenantMapper {
    private static final String DOCUMENT_DIRECT_PATH = "api/document/resource";
    protected static final String DOCUMENT_LINK_PATH = "api/application/links";
    private static final String PREVIEW_PATH = "api/file/preview";
    private static final String DOSSIER_PDF_PATH = "api/application/fullPdf";
    private static final String DOSSIER_PATH = "file";

    @Value("${application.base.url}")
    protected String applicationBaseUrl;

    @Value("${api.failed.rules.min.level:WARN}")
    protected DocumentRuleLevel minBrokenRulesLevel;

    @Value("${tenant.base.url}")
    protected String tenantBaseUrl;

    @Mapping(source = "tenant", target = "franceConnectIdentity", qualifiedByName = "franceConnectIdentity")
    public abstract TenantModel toTenantModel(Tenant tenant, @Context UserApi userApi);

    @Named("franceConnectIdentity")
    FranceConnectIdentity franceConnectIdentity(Tenant item) {
        if (item.getFranceConnect()) {
            return FranceConnectIdentity.builder()
                    .firstName(item.getUserFirstName())
                    .lastName(item.getUserLastName())
                    .preferredName(item.getUserPreferredName())
                    .build();
        }
        return null;
    }

    @Mapping(target = "name", expression = "java(buildDocumentUrl(document, userApi))")
    @Mapping(target = "files", expression = "java((userApi == null)? mapFiles(document.getFiles()) : null)")
    @MapDocumentCategories
    public abstract DocumentModel toDocumentModel(Document document, @Context UserApi userApi);

    protected String buildDocumentUrl(Document document, UserApi userApi) {
        if (document.getWatermarkFile() == null) {
            return null;
        }
        String token = resolvePartnerToken(document, userApi);
        if (token != null) {
            return UriComponentsBuilder
                    .fromUriString(applicationBaseUrl)
                    .path(DOCUMENT_LINK_PATH)
                    .path("/{token}/documents/{name}")
                    .buildAndExpand(token, document.getName())
                    .toUriString();
        }
        return UriComponentsBuilder
                .fromUriString(applicationBaseUrl)
                .path(DOCUMENT_DIRECT_PATH)
                .path("/{name}")
                .buildAndExpand(document.getName())
                .toUriString();
    }

    public abstract List<FileModel> mapFiles(List<File> files);

    @Mapping(target = "connectedTenantId", source = "id")
    public abstract ConnectedTenantModel toTenantModelDfc(Tenant tenant, @Context UserApi userApi);

    @Mapping(target = "preview", expression = "java(buildPreviewUrl(documentFile))")
    @Mapping(target = "size", source = "documentFile.storageFile.size")
    @Mapping(target = "contentType", source = "documentFile.storageFile.contentType")
    @Mapping(target = "originalName", source = "documentFile.storageFile.name")
    @Mapping(target = "md5", source = "documentFile.storageFile.md5")
    public abstract FileModel toFileModel(File documentFile);

    protected String buildPreviewUrl(File documentFile) {
        if( documentFile.getPreview() == null) {
            return null;
        }
        return UriComponentsBuilder
                .fromUriString(applicationBaseUrl)
                .path(PREVIEW_PATH)
                .path(documentFile.getId().toString())
                .build()
                .toUriString();
    }

    @AfterMapping
    void modificationsAfterMapping(@MappingTarget TenantModel.TenantModelBuilder tenantModelBuilder, @Context UserApi userApi, Tenant tenant) {
        TenantModel tenantModel = tenantModelBuilder.build();
        ApartmentSharingModel apartmentSharingModel = tenantModel.getApartmentSharing();
        if (userApi != null) {
            updateTokens(tenant, apartmentSharingModel,
                    l -> l.getLinkType() == ApartmentSharingLinkType.PARTNER
                            && Objects.equals(l.getPartnerId(), userApi.getId()));
        } else {
            updateTokens(tenant, apartmentSharingModel, l -> l.getLinkType() == ApartmentSharingLinkType.LINK);
        }

        var isDossierUser = SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_dossier"));
        var filePath = isDossierUser ? "/api/file/resource/" : null;

        // We hide the analysis report broken rules of info level
        hideDocumentAnalysisReportInfoLevel(tenantModel.getDocuments());

        setDocumentDeniedReasonsAndDocumentAndFilesRoutes(tenantModel.getDocuments(), filePath, false);

        tenantModel.getApartmentSharing().getTenants().stream().filter(t -> Objects.equals(t.getId(), tenantModel.getId())).forEach(
                t -> {
                    t.setDocuments(null);
                    t.setGuarantors(null);
                }
        );
        Optional.ofNullable(tenantModel.getApartmentSharing().getTenants())
                .ifPresent(coTenantModels -> coTenantModels.forEach(coTenantModel -> {
                    // If the tenant is a couple, we want to show the documents of the other tenant
                    // Otherwise we only load the preview.
                    setDocumentDeniedReasonsAndDocumentAndFilesRoutes(
                            coTenantModel.getDocuments(),
                            filePath,
                            tenantModel.getApartmentSharing().getApplicationType() != ApplicationType.COUPLE
                    );
                    // We hide the analysis report broken rules of info level
                    hideDocumentAnalysisReportInfoLevel(coTenantModel.getDocuments());
                    Optional.ofNullable(coTenantModel.getGuarantors())
                            .ifPresent(guarantorModels -> guarantorModels.forEach(guarantorModel -> setDocumentDeniedReasonsAndDocumentAndFilesRoutes(guarantorModel.getDocuments(), filePath, true)));
                }));

        Optional.ofNullable(tenantModel.getGuarantors())
                .ifPresent(guarantorModels -> guarantorModels.forEach(guarantorModel -> setDocumentDeniedReasonsAndDocumentAndFilesRoutes(guarantorModel.getDocuments(), filePath, false)));

    }

    private void updateTokens(Tenant tenant, BaseApartmentSharingModel apartmentSharingModel, Predicate<ApartmentSharingLink> isRightLink) {
        String token = null;
        String tokenPublic = null;
        if (apartmentSharingModel.getStatus() == TenantFileStatus.VALIDATED) {
            List<ApartmentSharingLink> links = tenant.getApartmentSharing().getApartmentSharingLinks();
            if (links != null) {
                Optional<ApartmentSharingLink> fullLink = links.stream()
                    .filter(l -> isRightLink.test(l) && l.isFullData())
                    .findFirst();
                Optional<ApartmentSharingLink> link = links.stream()
                    .filter(l -> isRightLink.test(l) && !l.isFullData())
                    .findFirst();
                if (fullLink.isPresent()) {
                    token = fullLink.get().getToken().toString();
                    apartmentSharingModel.setDossierPdfUrl(applicationBaseUrl + "/" + DOSSIER_PDF_PATH + "/" + token);
                    apartmentSharingModel.setDossierUrl(tenantBaseUrl + "/" + DOSSIER_PATH + "/" + token);
                }
                if (link.isPresent()) {
                    tokenPublic = link.get().getToken().toString();
                }
            }
        }
        apartmentSharingModel.setToken(token);
        apartmentSharingModel.setTokenPublic(tokenPublic);
    }

    private void setDocumentDeniedReasonsAndDocumentAndFilesRoutes(List<DocumentModel> list, String filePath, boolean previewOnly) {
        Optional.ofNullable(list)
                .ifPresent(documentModels -> documentModels.forEach(documentModel -> {
                    DocumentDeniedReasonsModel documentDeniedReasonsModel = documentModel.getDocumentDeniedReasons();
                    if (documentDeniedReasonsModel != null) {
                        List<SelectedOption> selectedOptionList = new ArrayList<>();
                        if (documentDeniedReasonsModel.isMessageData()) {
                            for (int i = 0; i < documentDeniedReasonsModel.getCheckedOptions().size(); i++) {
                                String checkedOption = documentDeniedReasonsModel.getCheckedOptions().get(i);
                                Integer checkedOptionsId = documentDeniedReasonsModel.getCheckedOptionsId().get(i);
                                selectedOptionList.add(SelectedOption.builder()
                                        .id(checkedOptionsId)
                                        .label(checkedOption).build());
                            }
                        } else {
                            for (int i = 0; i < documentDeniedReasonsModel.getCheckedOptions().size(); i++) {
                                String checkedOption = documentDeniedReasonsModel.getCheckedOptions().get(i);
                                selectedOptionList.add(SelectedOption.builder().id(null).label(checkedOption).build());
                            }
                        }
                        documentDeniedReasonsModel.setSelectedOptions(selectedOptionList);
                        documentDeniedReasonsModel.setCheckedOptions(null);
                        documentDeniedReasonsModel.setCheckedOptionsId(null);
                        documentModel.setDocumentDeniedReasons(documentDeniedReasonsModel);
                    }
                    Optional.ofNullable(documentModel.getFiles())
                            .ifPresent(fileModels -> fileModels.forEach(fileModel -> {
                                if (filePath == null || previewOnly) {
                                    fileModel.setPath("");
                                } else {
                                    fileModel.setPath(applicationBaseUrl + filePath + fileModel.getId());
                                }
                            }));
                }));
    }

    @AfterMapping
    void modificationsAfterMapping(@MappingTarget ConnectedTenantModel.ConnectedTenantModelBuilder connectedTenantModelBuilder, @Context UserApi userApi, Tenant tenant) {
        ConnectedTenantModel connectedTenantModel = connectedTenantModelBuilder.build();
        fr.dossierfacile.api.front.model.dfc.apartment_sharing.ApartmentSharingModel apartmentSharingModel = connectedTenantModel.getApartmentSharing();
        updateTokens(tenant, apartmentSharingModel, l -> l.getLinkType() == ApartmentSharingLinkType.PARTNER && userApi != null && Objects.equals(l.getPartnerId(), userApi.getId()));

        connectedTenantModel.getApartmentSharing().getTenants().forEach(tenantModel -> setDocumentDeniedReasonsAndDocumentAndFilesRoutes(tenantModel.getDocuments(), null, true));
        connectedTenantModel.getApartmentSharing().getTenants().forEach(tenantModel ->
                Optional.ofNullable(tenantModel.getGuarantors()).ifPresent(guarantorModels ->
                        guarantorModels.forEach(guarantorModel -> setDocumentDeniedReasonsAndDocumentAndFilesRoutes(guarantorModel.getDocuments(), null, true))));
    }

    private String resolvePartnerToken(Document document, UserApi userApi) {
        if (userApi == null) {
            return null;
        }
        ApartmentSharing apartmentSharing = getApartmentSharing(document);
        if (apartmentSharing == null || apartmentSharing.getStatus() != TenantFileStatus.VALIDATED) {
            return null;
        }
        List<ApartmentSharingLink> links = apartmentSharing.getApartmentSharingLinks();
        if (links == null) {
            return null;
        }
        return links.stream()
                .filter(l -> l.getLinkType() == ApartmentSharingLinkType.PARTNER
                        && Objects.equals(l.getPartnerId(), userApi.getId())
                        && l.isFullData())
                .findFirst()
                .map(l -> l.getToken().toString())
                .orElse(null);
    }

    private ApartmentSharing getApartmentSharing(Document document) {
        if (document.getTenant() != null) {
            return document.getTenant().getApartmentSharing();
        }
        if (document.getGuarantor() != null && document.getGuarantor().getTenant() != null) {
            return document.getGuarantor().getTenant().getApartmentSharing();
        }
        return null;
    }

    private void hideDocumentAnalysisReportInfoLevel(List<DocumentModel> documents) {
        if (documents != null) {
            documents.forEach(it -> removeInfoAnalysisReportBrokenRules(it.getDocumentAnalysisReport()));
        }
    }

    private void removeInfoAnalysisReportBrokenRules(DocumentAnalysisReportModel documentAnalysisReport) {

        if (documentAnalysisReport != null) {
            documentAnalysisReport.setFailedRules(
                    documentAnalysisReport.getFailedRules().stream().filter(it -> it.getLevel().ordinal() >= minBrokenRulesLevel.ordinal())
                            .toList()
            );
        }

    }

}

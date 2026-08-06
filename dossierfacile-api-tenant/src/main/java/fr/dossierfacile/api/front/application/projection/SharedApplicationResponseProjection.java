package fr.dossierfacile.api.front.application.projection;

import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.guarantor.Guarantor;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import fr.dossierfacile.common.model.apartment_sharing.GuarantorModel;
import fr.dossierfacile.common.model.apartment_sharing.TenantModel;

import java.util.List;
import java.util.function.Function;

/**
 * Common mapping of the loaded aggregates into the shared ApplicationModel DTO
 */
abstract class SharedApplicationResponseProjection {

    protected ApplicationModel assemble(ApplicationProjectionSources sources, Function<Document, String> documentName) {
        return ApplicationModel.builder()
                .id(sources.apartmentSharing().getId())
                .applicationType(sources.apartmentSharing().getApplicationType())
                .dossierPdfDocumentStatus(sources.apartmentSharing().getDossierPdfDocumentStatus())
                .status(sources.aggregatedStatus())
                .lastUpdateDate(sources.lastUpdateDate())
                .tenants(sources.tenants().stream()
                        .map(tenant -> toTenantModel(tenant, sources, documentName))
                        .toList())
                .build();
    }

    private TenantModel toTenantModel(Tenant tenant, ApplicationProjectionSources sources,
                                      Function<Document, String> documentName) {
        return TenantModel.builder()
                .id(tenant.getId())
                .firstName(tenant.getFirstName())
                .lastName(tenant.getLastName())
                .preferredName(tenant.getPreferredName())
                .zipCode(tenant.getZipCode())
                .abroad(tenant.getAbroad())
                .email(tenant.getEmail())
                .tenantType(tenant.getTenantType())
                .franceConnect(tenant.getFranceConnect())
                .ownerType(tenant.getOwnerType())
                .status(sources.statusByTenantId().get(tenant.getId()))
                .honorDeclaration(tenant.getHonorDeclaration())
                .clarification(tenant.getClarification())
                .lastUpdateDate(tenant.getLastUpdateDate())
                .documents(toDocumentModels(sources.documentsByTenantId().getOrDefault(tenant.getId(), List.of()), documentName))
                .guarantors(sources.guarantorsByTenantId().getOrDefault(tenant.getId(), List.of()).stream()
                        .map(guarantor -> toGuarantorModel(guarantor, sources, documentName))
                        .toList())
                .build();
    }

    private GuarantorModel toGuarantorModel(Guarantor guarantor, ApplicationProjectionSources sources,
                                            Function<Document, String> documentName) {
        return GuarantorModel.builder()
                .id(guarantor.getId())
                .firstName(guarantor.getFirstName())
                .lastName(guarantor.getLastName())
                .legalPersonName(guarantor.getLegalPersonName())
                .typeGuarantor(guarantor.getTypeGuarantor())
                .documents(toDocumentModels(sources.documentsByGuarantorId().getOrDefault(guarantor.getId(), List.of()), documentName))
                .build();
    }

    private List<DocumentModel> toDocumentModels(List<Document> documents, Function<Document, String> documentName) {
        return documents.stream()
                .map(document -> DocumentModel.builder()
                        .id(document.getId())
                        .documentCategory(document.getDocumentCategory())
                        .documentSubCategory(document.getDocumentSubCategory())
                        .subCategory(document.getDocumentSubCategory())
                        .documentCategoryStep(document.getDocumentCategoryStep())
                        .customText(document.getCustomText())
                        .monthlySum(document.getMonthlySum())
                        .documentStatus(document.getDocumentStatus())
                        .name(documentName.apply(document))
                        .build())
                .toList();
    }
}

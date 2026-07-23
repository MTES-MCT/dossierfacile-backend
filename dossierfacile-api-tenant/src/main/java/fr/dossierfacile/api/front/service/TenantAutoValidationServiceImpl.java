package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.TenantAutoValidationService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class TenantAutoValidationServiceImpl implements TenantAutoValidationService {

    @Override
    public boolean isEligibleForAutoValidation(Document document) {
        if (document == null || document.getDocumentSubCategory() == null) {
            return false;
        }
        return document.getDocumentSubCategory() == DocumentSubCategory.VISALE;
    }

    @Override
    public boolean isTenantReadyForAutoValidation(Tenant tenant) {
        if (tenant == null) {
            return false;
        }

        if (!Boolean.TRUE.equals(tenant.getHonorDeclaration()) || !tenant.isAllCategories()) {
            return false;
        }

        List<Document> allDocuments = (tenant.getGuarantors() == null) ?
                tenant.getDocuments() :
                Stream.concat(
                        tenant.getDocuments() != null ? tenant.getDocuments().stream() : Stream.empty(),
                        tenant.getGuarantors().stream()
                                .map(Guarantor::getDocuments)
                                .filter(Objects::nonNull)
                                .flatMap(List::stream)
                ).toList();

        if (allDocuments == null || allDocuments.isEmpty()) {
            return false;
        }

        if (allDocuments.stream().anyMatch(d -> d.getDocumentStatus() == DocumentStatus.DECLINED)) {
            return false;
        }

        List<Document> toProcessDocuments = allDocuments.stream()
                .filter(d -> d.getDocumentStatus() == DocumentStatus.TO_PROCESS)
                .toList();

        if (toProcessDocuments.isEmpty()) {
            return false;
        }

        return toProcessDocuments.stream().allMatch(this::isEligibleForAutoValidation);
    }

}

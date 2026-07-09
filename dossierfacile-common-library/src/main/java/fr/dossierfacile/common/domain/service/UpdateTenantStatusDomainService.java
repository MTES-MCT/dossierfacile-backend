package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.domain.event.TenantStatusChangedEvent;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.guarantor.Guarantor;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaGuarantorRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateTenantStatusDomainService {

    private final JpaTenantRepository jpaTenantRepository;
    private final JpaDocumentRepository jpaDocumentRepository;
    private final JpaGuarantorRepository jpaGuarantorRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private final List<DocumentCategory> mandatoryCategoriesForTenantAndGuarantorMandatoryCategories = List.of(
            DocumentCategory.IDENTIFICATION,
            DocumentCategory.RESIDENCY,
            DocumentCategory.PROFESSIONAL,
            DocumentCategory.FINANCIAL,
            DocumentCategory.TAX
    );

    public UpdateTenantStatusResult updateTenantStatus(Tenant tenant) {
        return updateTenantStatus(tenant, null);
    }

    public UpdateTenantStatusResult updateTenantStatus(Tenant tenant, User operator) {
        var previousStatus = tenant.getStatus();
        TenantFileStatus newStatus = computeTenantStatus(tenant);

        if (previousStatus != newStatus) {
            tenant.setStatus(newStatus);
            jpaTenantRepository.save(tenant);
            
            // Publish domain event
            eventPublisher.publishEvent(new TenantStatusChangedEvent(
                    tenant.getId(),
                    previousStatus,
                    newStatus,
                    operator
            ));
        } else {
            tenant.setStatus(newStatus);
            jpaTenantRepository.save(tenant);
        }

        return new UpdateTenantStatusResult(
                previousStatus != newStatus,
                tenant.getId(),
                newStatus
        );
    }

    // J'ai décidé de le mettre ici et pas dans l'aggregat car on a besoin de trop d'informations et que ça devient ingérable
    public TenantFileStatus computeTenantStatus(Tenant tenant) {
        log.info("Computing status for tenant with ID [" + tenant.getId() + "]...");

        if (tenant.getStatus() == TenantFileStatus.ARCHIVED) {
            return TenantFileStatus.ARCHIVED;
        }

        List<Document> tenantDocuments = jpaDocumentRepository.getDocumentsByTenantId(tenant.getId());
        List<Guarantor> guarantors = jpaGuarantorRepository.findByTenantId(tenant.getId());
        List<Long> guarantorIds = guarantors.stream().map(Guarantor::getId).toList();
        List<Document> guarantorDocuments = jpaDocumentRepository.getDocumentsByGuarantorsIds(guarantorIds);
        List<Document> allDocuments = ListUtils.union(tenantDocuments, guarantorDocuments);

        if (allDocuments.stream().anyMatch(d -> d.getDocumentStatus() == DocumentStatus.DECLINED)) {
            return TenantFileStatus.DECLINED;
        } else if (allDocuments.stream().anyMatch(d -> d.getDocumentStatus() == DocumentStatus.TO_PROCESS)) {
            return TenantFileStatus.TO_PROCESS;
        } else if (!Boolean.TRUE.equals(tenant.getHonorDeclaration())
                || !isAllCategoriesPresentInTenant(tenantDocuments)
                || !areAllGuarantorsComplete(guarantors, guarantorDocuments)) {
            return TenantFileStatus.INCOMPLETE;
        }

        return TenantFileStatus.VALIDATED;
    }

    private boolean isAllCategoriesPresentInTenant(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return false;
        }
        List<DocumentCategory> existingCategories = documents.stream()
                .map(Document::getDocumentCategory)
                .toList();

        return new HashSet<>(existingCategories).containsAll(mandatoryCategoriesForTenantAndGuarantorMandatoryCategories);
    }

    private boolean areAllGuarantorsComplete(List<Guarantor> guarantors, List<Document> guarantorDocuments) {
        if (guarantors == null || guarantors.isEmpty()) {
            return true;
        }

        for (Guarantor guarantor : guarantors) {
            List<Document> documentsForGuarantor = guarantorDocuments.stream()
                    .filter(d -> guarantor.getId().equals(d.getGuarantorId()))
                    .toList();

            if (documentsForGuarantor.isEmpty()) {
                return false;
            }

            if (guarantor.getTypeGuarantor() == TypeGuarantor.ORGANISM) {
                // Must have exactly one Document of type GUARANTEE_PROVIDER_CERTIFICATE
                if (documentsForGuarantor.size() != 1 || documentsForGuarantor.getFirst().getDocumentCategory() != DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE) {
                    return false;
                }
            } else if (guarantor.getTypeGuarantor() == TypeGuarantor.NATURAL_PERSON) {
                List<DocumentCategory> existing = documentsForGuarantor.stream()
                        .map(Document::getDocumentCategory)
                        .toList();
                if (!new HashSet<>(existing).containsAll(mandatoryCategoriesForTenantAndGuarantorMandatoryCategories)) {
                    return false;
                }
            } else if (guarantor.getTypeGuarantor() == TypeGuarantor.LEGAL_PERSON) {
                List<DocumentCategory> mandatory = List.of(
                        DocumentCategory.IDENTIFICATION,
                        DocumentCategory.IDENTIFICATION_LEGAL_PERSON
                );
                List<DocumentCategory> existing = documentsForGuarantor.stream()
                        .map(Document::getDocumentCategory)
                        .toList();
                if (!new HashSet<>(existing).containsAll(mandatory)) {
                    return false;
                }
            }
        }
        return true;
    }

    public record UpdateTenantStatusResult(
            boolean hasBeenUpdated,
            Long tenantId,
            TenantFileStatus newStatus
    ) {
    }
}

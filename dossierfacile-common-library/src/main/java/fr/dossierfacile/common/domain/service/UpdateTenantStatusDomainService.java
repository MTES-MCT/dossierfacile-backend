package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.domain.event.TenantStatusChangedEvent;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.guarantor.Guarantor;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaGuarantorRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateTenantStatusDomainService {

    private final JpaTenantRepository jpaTenantRepository;
    private final JpaDocumentRepository jpaDocumentRepository;
    private final JpaGuarantorRepository jpaGuarantorRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final AddLogDomainService addLogDomainService;

    private static final List<DocumentCategory> TENANT_OR_NATURAL_GUARANTOR_MANDATORY_CATEGORIES = List.of(
            DocumentCategory.IDENTIFICATION,
            DocumentCategory.RESIDENCY,
            DocumentCategory.PROFESSIONAL,
            DocumentCategory.FINANCIAL,
            DocumentCategory.TAX
    );

    private static final List<DocumentCategory> LEGAL_PERSON_GUARANTOR_MANDATORY_CATEGORIES = List.of(
            DocumentCategory.IDENTIFICATION,
            DocumentCategory.IDENTIFICATION_LEGAL_PERSON
    );

    /** Minimal projection of a document for the status computation. */
    public record DocumentView(DocumentStatus status, DocumentCategory category) {}

    /** Minimal projection of a guarantor and its documents for the status computation. */
    public record GuarantorView(TypeGuarantor type, List<DocumentView> documents) {}

    public UpdateTenantStatusResult updateTenantStatus(Tenant tenant) {
        return updateTenantStatus(tenant, null);
    }

    public UpdateTenantStatusResult updateTenantStatus(Tenant tenant, User operator) {
        var previousStatus = tenant.getStatus();
        TenantFileStatus newStatus = computeTenantStatus(tenant);

        if (previousStatus != newStatus) {
            tenant.setStatus(newStatus);
            jpaTenantRepository.save(tenant);
            
            if (newStatus == TenantFileStatus.VALIDATED) {
                addLogDomainService.addAccountValidatedLog(tenant, Optional.ofNullable(operator));
            } else if (newStatus == TenantFileStatus.DECLINED) {
                addLogDomainService.addAccountDeniedLog(tenant, Optional.ofNullable(operator));
            }

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

        // Short-circuit before loading anything: ARCHIVED is sticky
        if (tenant.getStatus() == TenantFileStatus.ARCHIVED) {
            return TenantFileStatus.ARCHIVED;
        }

        List<Document> tenantDocuments = jpaDocumentRepository.getDocumentsByTenantId(tenant.getId());
        List<Guarantor> guarantors = jpaGuarantorRepository.findByTenantId(tenant.getId());
        List<Long> guarantorIds = guarantors.stream().map(Guarantor::getId).toList();
        Map<Long, List<Document>> documentsByGuarantorId = jpaDocumentRepository.getDocumentsByGuarantorsIds(guarantorIds).stream()
                .collect(Collectors.groupingBy(Document::getGuarantorId));

        return computeStatus(
                tenant.getStatus(),
                Boolean.TRUE.equals(tenant.getHonorDeclaration()),
                toDocumentViews(tenantDocuments),
                guarantors.stream()
                        .map(guarantor -> new GuarantorView(
                                guarantor.getTypeGuarantor(),
                                toDocumentViews(documentsByGuarantorId.getOrDefault(guarantor.getId(), List.of()))))
                        .toList());
    }

    private static List<DocumentView> toDocumentViews(List<Document> documents) {
        return documents.stream()
                .map(document -> new DocumentView(document.getDocumentStatus(), document.getDocumentCategory()))
                .toList();
    }

    public static TenantFileStatus computeStatus(TenantFileStatus storedStatus,
                                                 boolean honorDeclaration,
                                                 List<DocumentView> tenantDocuments,
                                                 List<GuarantorView> guarantors) {
        if (storedStatus == TenantFileStatus.ARCHIVED) {
            return TenantFileStatus.ARCHIVED;
        }

        List<DocumentView> allDocuments = Stream.concat(
                        tenantDocuments.stream(),
                        guarantors.stream().flatMap(guarantor -> guarantor.documents().stream()))
                .toList();

        if (allDocuments.stream().anyMatch(document -> document.status() == DocumentStatus.DECLINED)) {
            return TenantFileStatus.DECLINED;
        }
        if (!honorDeclaration || !isComplete(tenantDocuments, guarantors)) {
            return TenantFileStatus.INCOMPLETE;
        }
        if (allDocuments.stream().anyMatch(document -> document.status() == DocumentStatus.TO_PROCESS)) {
            return TenantFileStatus.TO_PROCESS;
        }
        return TenantFileStatus.VALIDATED;
    }

    /** Completeness check shared with entity/Tenant#isAllCategories. */
    public static boolean isComplete(List<DocumentView> tenantDocuments, List<GuarantorView> guarantors) {
        List<DocumentCategory> tenantCategories = tenantDocuments.stream()
                .map(DocumentView::category)
                .toList();
        if (!tenantCategories.containsAll(TENANT_OR_NATURAL_GUARANTOR_MANDATORY_CATEGORIES)) {
            return false;
        }

        for (GuarantorView guarantor : guarantors) {
            if (guarantor.documents().isEmpty()) {
                return false;
            }
            if (guarantor.type() == null) {
                // Legacy behavior: guarantors matching no known type are skipped
                continue;
            }
            boolean complete = switch (guarantor.type()) {
                // An ORGANISM guarantor must have exactly one GUARANTEE_PROVIDER_CERTIFICATE document
                case ORGANISM -> guarantor.documents().size() == 1
                        && guarantor.documents().getFirst().category() == DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE;
                case NATURAL_PERSON -> categoriesOf(guarantor.documents())
                        .containsAll(TENANT_OR_NATURAL_GUARANTOR_MANDATORY_CATEGORIES);
                case LEGAL_PERSON -> categoriesOf(guarantor.documents())
                        .containsAll(LEGAL_PERSON_GUARANTOR_MANDATORY_CATEGORIES);
            };
            if (!complete) {
                return false;
            }
        }
        return true;
    }

    private static List<DocumentCategory> categoriesOf(List<DocumentView> documents) {
        return documents.stream()
                .map(DocumentView::category)
                .toList();
    }

    public record UpdateTenantStatusResult(
            boolean hasBeenUpdated,
            Long tenantId,
            TenantFileStatus newStatus
    ) {
    }
}

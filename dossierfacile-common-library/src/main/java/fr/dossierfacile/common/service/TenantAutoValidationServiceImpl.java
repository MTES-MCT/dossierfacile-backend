package fr.dossierfacile.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.AutoValidationResultStatus;
import fr.dossierfacile.common.enums.DocumentAutoValidationReason;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.log.AutoValidationDocumentDetail;
import fr.dossierfacile.common.model.log.AutoValidationLogDetails;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.TenantAutoValidationService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantAutoValidationServiceImpl implements TenantAutoValidationService {

    private final TenantCommonRepository tenantCommonRepository;
    private final DocumentCommonRepository documentCommonRepository;
    private final TenantCommonService tenantCommonService;
    private final TenantLogCommonService tenantLogCommonService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        List<Document> allDocuments = getAllDocuments(tenant);

        if (allDocuments.isEmpty()) {
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

    @Override
    public List<Tenant> listTenantsToAutoValidate(LocalDateTime maxLastUpdateDate) {
        return tenantCommonRepository.findTenantsToAutoValidate(maxLastUpdateDate);
    }

    @Override
    @Transactional
    public boolean processAutoValidationForTenant(Long tenantId) {
        Tenant tenant = tenantCommonRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            log.warn("Attempted auto-validation for non-existent tenant ID [{}]", tenantId);
            return false;
        }

        if (!Boolean.TRUE.equals(tenant.getReadyForAutoValidation()) || tenant.getStatus() != TenantFileStatus.TO_PROCESS) {
            log.info("Tenant ID [{}] is no longer ready for auto-validation or status changed from TO_PROCESS", tenantId);
            return false;
        }

        log.info("Processing auto-validation for tenant ID [{}]", tenantId);

        List<Document> allDocuments = getAllDocuments(tenant);

        List<Document> toProcessDocuments = allDocuments.stream()
                .filter(d -> d.getDocumentStatus() == DocumentStatus.TO_PROCESS)
                .toList();

        if (toProcessDocuments.isEmpty()) {
            log.info("Tenant ID [{}] has no documents in TO_PROCESS status", tenantId);
            tenant.setReadyForAutoValidation(false);
            tenantCommonRepository.save(tenant);

            AutoValidationLogDetails logDetails = AutoValidationLogDetails.builder()
                    .status(AutoValidationResultStatus.NO_DOCUMENTS)
                    .documents(List.of())
                    .build();

            saveAutoValidationLog(tenant.getId(), LogType.ACCOUNT_AUTO_VALIDATION_FAILED, logDetails);
            return false;
        }

        List<AutoValidationDocumentDetail> documentDetailsList = new ArrayList<>();
        boolean allPassed = true;

        for (Document doc : toProcessDocuments) {
            DocumentAutoValidationReason reason = evaluateDocumentReason(doc);

            if (reason != DocumentAutoValidationReason.VALIDATED) {
                allPassed = false;
                log.info("Tenant ID [{}] document ID [{}] auto-validation reason: [{}]", tenantId, doc.getId(), reason);
            }

            documentDetailsList.add(AutoValidationDocumentDetail.builder()
                    .documentId(doc.getId())
                    .documentCategory(doc.getDocumentCategory())
                    .documentSubCategory(doc.getDocumentSubCategory())
                    .documentCategoryStep(doc.getDocumentCategoryStep())
                    .reason(reason)
                    .build());
        }

        if (allPassed) {
            log.info("Tenant ID [{}] successfully auto-validated! Validating documents and updating status.", tenantId);

            for (Document doc : toProcessDocuments) {
                doc.setDocumentStatus(DocumentStatus.VALIDATED);
                documentCommonRepository.save(doc);
            }

            tenant.setReadyForAutoValidation(false);
            tenantCommonRepository.save(tenant);

            tenantCommonService.changeTenantStatusToValidated(tenant);
            tenantLogCommonService.saveTenantLog(new TenantLog(LogType.ACCOUNT_VALIDATED, tenant.getId()));

            AutoValidationLogDetails logDetails = AutoValidationLogDetails.builder()
                    .status(AutoValidationResultStatus.VALIDATED)
                    .documents(documentDetailsList)
                    .build();

            saveAutoValidationLog(tenant.getId(), LogType.ACCOUNT_AUTOMATICALLY_VALIDATED, logDetails);
            return true;
        } else {
            log.info("Tenant ID [{}] auto-validation checks failed - unflagging for human operators", tenantId);
            tenant.setReadyForAutoValidation(false);
            tenantCommonRepository.save(tenant);

            AutoValidationLogDetails logDetails = AutoValidationLogDetails.builder()
                    .status(AutoValidationResultStatus.FAILED)
                    .documents(documentDetailsList)
                    .build();

            saveAutoValidationLog(tenant.getId(), LogType.ACCOUNT_AUTO_VALIDATION_FAILED, logDetails);
            return false;
        }
    }

    private List<Document> getAllDocuments(Tenant tenant) {
        if (tenant == null) {
            return List.of();
        }

        Stream<Document> tenantDocsStream = (tenant.getDocuments() != null) ?
                tenant.getDocuments().stream() :
                Stream.empty();

        if (tenant.getGuarantors() == null || tenant.getGuarantors().isEmpty()) {
            return tenantDocsStream.toList();
        }

        Stream<Document> guarantorDocsStream = tenant.getGuarantors().stream()
                .map(Guarantor::getDocuments)
                .filter(Objects::nonNull)
                .flatMap(List::stream);

        return Stream.concat(tenantDocsStream, guarantorDocsStream).toList();
    }

    private DocumentAutoValidationReason evaluateDocumentReason(Document doc) {
        if (!isEligibleForAutoValidation(doc)) {
            return DocumentAutoValidationReason.DOCUMENT_NOT_ELIGIBLE;
        }

        DocumentAnalysisReport report = doc.getDocumentAnalysisReport();
        if (report == null) {
            return DocumentAutoValidationReason.REPORT_MISSING;
        }

        if (report.getAnalysisStatus() != DocumentAnalysisStatus.CHECKED) {
            return DocumentAutoValidationReason.REPORT_NOT_CHECKED;
        }

        if (!CollectionUtils.isEmpty(report.getFailedRules())) {
            return DocumentAutoValidationReason.FAILED_RULES_PRESENT;
        }

        if (!CollectionUtils.isEmpty(report.getInconclusiveRules())) {
            return DocumentAutoValidationReason.INCONCLUSIVE_RULES_PRESENT;
        }

        if (CollectionUtils.isEmpty(report.getPassedRules())) {
            return DocumentAutoValidationReason.NO_PASSED_RULES;
        }

        return DocumentAutoValidationReason.VALIDATED;
    }

    private void saveAutoValidationLog(Long tenantId, LogType logType, AutoValidationLogDetails logDetailsDto) {
        ObjectNode jsonNode = objectMapper.valueToTree(logDetailsDto);
        TenantLog tenantLog = TenantLog.builder()
                .logType(logType)
                .tenantId(tenantId)
                .creationDateTime(LocalDateTime.now(ZoneId.systemDefault()))
                .logDetails(jsonNode)
                .build();
        tenantLogCommonService.saveTenantLog(tenantLog);
    }

}

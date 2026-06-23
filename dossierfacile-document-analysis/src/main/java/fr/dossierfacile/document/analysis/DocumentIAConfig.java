package fr.dossierfacile.document.analysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.document.analysis.external.documentia.WorkflowV2StepParamOverride;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentIAConfig {

    private final FeatureFlagService featureFlagService;


    @Value("${document.ia.api.default.workflow.id:document-classification-extraction-v2}")
    private String defaultWorkflowId;

    private static final String FEATURE_FLAG_TAX_ANALYSIS_KEY = "document-ia-tax-analysis";
    private static final String FEATURE_FLAG_VISALE_ANALYSIS_KEY = "document-ia-visale-analysis";
    private static final String FEATURE_FLAG_SALARY_MORE_3_MONTHS_ANALYSIS_KEY = "document-ia-salary-more-3-months-analysis";
    private static final String FEATURE_FLAG_RESIDENCY_ANALYSIS_KEY = "document-ia-residency-classification";
    private static final String FEATURE_FLAG_PROPERTY_TAX_ANALYSIS_KEY = "document-ia-property-tax-analysis";

    @Data
    public static class WorkflowConfig {
        private String workflowId;
        private Map<String, List<WorkflowV2StepParamOverride>> overrides = new HashMap<>();

        public WorkflowConfig(String workflowId) {
            this.workflowId = workflowId;
        }

        public WorkflowConfig(String workflowId, Map<String, List<WorkflowV2StepParamOverride>> overrides) {
            this.workflowId = workflowId;
            this.overrides = overrides;
        }
    }

    public boolean hasToSendFileForAnalysis(Document document, long tenantId) {
        if (document.getDocumentSubCategory() == DocumentSubCategory.MY_NAME) {
            return featureFlagService.isFeatureEnabledForUser(tenantId, FEATURE_FLAG_TAX_ANALYSIS_KEY) &&
                    document.getDocumentCategoryStep() == DocumentCategoryStep.TAX_FRENCH_NOTICE;
        }

        if (document.getDocumentSubCategory() == DocumentSubCategory.VISALE) {
            return featureFlagService.isFeatureEnabledForUser(tenantId, FEATURE_FLAG_VISALE_ANALYSIS_KEY);
        }

        if (document.getDocumentSubCategory() == DocumentSubCategory.SALARY) {
            return featureFlagService.isFeatureEnabledForUser(tenantId, FEATURE_FLAG_SALARY_MORE_3_MONTHS_ANALYSIS_KEY) &&
                    document.getDocumentCategoryStep() == DocumentCategoryStep.SALARY_EMPLOYED_MORE_3_MONTHS;
        }

        if (document.getDocumentSubCategory() == DocumentSubCategory.OWNER) {
            return featureFlagService.isFeatureEnabledForUser(tenantId, FEATURE_FLAG_PROPERTY_TAX_ANALYSIS_KEY);
        }

        if (List.of(DocumentSubCategory.TENANT, DocumentSubCategory.GUEST).contains(document.getDocumentSubCategory())) {
            return featureFlagService.isFeatureEnabledForUser(tenantId, FEATURE_FLAG_RESIDENCY_ANALYSIS_KEY);
        }

        return false;
    }

    public WorkflowConfig getWorkflowConfig(Document document) {
        return switch (document.getDocumentSubCategory()) {
            case MY_NAME -> new WorkflowConfig("document-barcode-extraction-v2");
            case TENANT, GUEST -> new WorkflowConfig("document-classification-v2", Map.of(
                    "extract_content_ocr", List.of(new WorkflowV2StepParamOverride("model", "tesseract"))
            ));
            default -> new WorkflowConfig(defaultWorkflowId);
        };
    }
}

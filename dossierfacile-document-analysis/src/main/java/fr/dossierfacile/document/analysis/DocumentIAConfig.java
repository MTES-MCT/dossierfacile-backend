package fr.dossierfacile.document.analysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentIAConfig {

    @Value("${document.ia.api.default.workflow.id:document-extraction-mistral-v1}")
    private String defaultWorkflowId;

    private final FeatureFlagService featureFlagService;

    private static final String FEATURE_FLAG_TAX_ANALYSIS_KEY = "document-ia-tax-analysis";
    private static final String FEATURE_FLAG_VISALE_ANALYSIS_KEY = "document-ia-visale-analysis";

    public boolean hasToSendFileForAnalysis(Document document, long tenantId) {
        if (List.of(DocumentSubCategory.MY_NAME, DocumentSubCategory.VISALE).contains(document.getDocumentSubCategory())) {
            if (
                    featureFlagService.isFeatureEnabledForUser(tenantId, FEATURE_FLAG_TAX_ANALYSIS_KEY) &&
                            document.getDocumentSubCategory() == DocumentSubCategory.MY_NAME &&
                            document.getDocumentCategoryStep() == DocumentCategoryStep.TAX_FRENCH_NOTICE
            ) {
                return true;
            }
            return featureFlagService.isFeatureEnabledForUser(tenantId, FEATURE_FLAG_VISALE_ANALYSIS_KEY) &&
                    document.getDocumentSubCategory() == DocumentSubCategory.VISALE;
        }
        return false;
    }

    public String getWorkflowIdForDocumentSubCategory(Document document) {
        //noinspection SwitchStatementWithTooFewBranches because we will add more workflows later
        return switch (document.getDocumentSubCategory()) {
            case MY_NAME -> "document-barcode-extraction";
            default -> defaultWorkflowId;
        };
    }

}

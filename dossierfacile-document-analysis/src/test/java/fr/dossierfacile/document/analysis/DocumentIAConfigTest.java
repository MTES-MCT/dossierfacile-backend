package fr.dossierfacile.document.analysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIAConfigTest {

    @Mock
    private FeatureFlagService featureFlagService;

    @InjectMocks
    private DocumentIAConfig documentIAConfig;

    private static final long TENANT_ID = 123L;

    @ParameterizedTest
    @EnumSource(value = DocumentSubCategory.class, names = {"CDI", "CDD", "ALTERNATION", "INTERNSHIP", "INTERMITTENT"})
    void should_send_for_analysis_when_professional_feature_flag_is_enabled(DocumentSubCategory subCategory) {
        Document document = Document.builder().documentSubCategory(subCategory).build();
        when(featureFlagService.isFeatureEnabledForUser(TENANT_ID, "document-ia-professional-analysis")).thenReturn(true);

        boolean result = documentIAConfig.hasToSendFileForAnalysis(document, TENANT_ID);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = DocumentSubCategory.class, names = {"CDI", "CDD", "ALTERNATION", "INTERNSHIP", "INTERMITTENT"})
    void should_not_send_for_analysis_when_professional_feature_flag_is_disabled(DocumentSubCategory subCategory) {
        Document document = Document.builder().documentSubCategory(subCategory).build();
        when(featureFlagService.isFeatureEnabledForUser(TENANT_ID, "document-ia-professional-analysis")).thenReturn(false);

        boolean result = documentIAConfig.hasToSendFileForAnalysis(document, TENANT_ID);

        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = DocumentSubCategory.class, names = {"CDI", "CDD", "ALTERNATION", "INTERNSHIP", "INTERMITTENT", "MY_NAME"})
    void should_return_barcode_workflow_for_professional_and_tax_documents(DocumentSubCategory subCategory) {
        Document document = Document.builder().documentSubCategory(subCategory).build();

        DocumentIAConfig.WorkflowConfig workflowConfig = documentIAConfig.getWorkflowConfig(document);

        assertThat(workflowConfig.getWorkflowId()).isEqualTo("document-barcode-extraction-v2");
    }
}

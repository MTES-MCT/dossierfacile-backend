package fr.dossierfacile.document.analysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocumentIAConfig {

    @Value("${document.ia.api.default.workflow.id:document-extraction-mistral-v1}")
    private String defaultWorkflowId;

    public boolean hasToSendFileForAnalysis(Document document) {
        return
                document.getDocumentSubCategory() == DocumentSubCategory.FRENCH_IDENTITY_CARD ||
                        document.getDocumentSubCategory() == DocumentSubCategory.SALARY ||
                        document.getDocumentSubCategory() == DocumentSubCategory.MY_NAME;

    }

    public String getWorkflowIdForDocumentSubCategory(Document document) {
        //noinspection SwitchStatementWithTooFewBranches because we will add more workflows later
        return switch (document.getDocumentSubCategory()) {
            case MY_NAME -> "document-barcode-extraction";
            default -> defaultWorkflowId;
        };
    }

}

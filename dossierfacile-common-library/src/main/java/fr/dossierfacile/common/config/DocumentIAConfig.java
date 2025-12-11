package fr.dossierfacile.common.config;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.stereotype.Component;

@Component
public class DocumentIAConfig {

    public boolean hasToSendFileForAnalysis(Document document) {
        return
                document.getDocumentSubCategory() == DocumentSubCategory.FRENCH_IDENTITY_CARD ||
                        document.getDocumentSubCategory() == DocumentSubCategory.MY_NAME ||
                        document.getDocumentSubCategory() == DocumentSubCategory.DRIVERS_LICENSE ||
                        document.getDocumentSubCategory() == DocumentSubCategory.FRENCH_PASSPORT ||
                        document.getDocumentSubCategory() == DocumentSubCategory.SALARY;
    }

}

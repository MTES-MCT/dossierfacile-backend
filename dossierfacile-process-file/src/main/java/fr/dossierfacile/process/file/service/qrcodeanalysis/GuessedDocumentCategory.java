package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIssuer;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.MonFranceConnectDocumentType;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;

import static fr.dossierfacile.common.enums.DocumentCategory.*;
import static fr.dossierfacile.common.enums.DocumentSubCategory.*;

public record GuessedDocumentCategory(
        DocumentCategory category,
        DocumentSubCategory subCategory
) {

    public static GuessedDocumentCategory forFile(InMemoryPdfFile pdfFile, DocumentIssuer issuerName) {
        return switch (issuerName) {
            case MON_FRANCE_CONNECT -> MonFranceConnectDocumentType.of(pdfFile).getCategory().orElse(null);
            case PAYFIT -> new GuessedDocumentCategory(FINANCIAL, SALARY);
            case DGFIP -> new GuessedDocumentCategory(TAX, MY_NAME);
            case UNKNOWN -> null;
        };
    }

    public boolean isMatchingCategoryOf(Document document) {
        return document.getDocumentCategory() == category &&
                document.getDocumentSubCategory() == subCategory;
    }

}
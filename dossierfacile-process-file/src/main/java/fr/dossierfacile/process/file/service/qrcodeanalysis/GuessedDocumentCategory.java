package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.MonFranceConnectDocumentType;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;

import java.util.Optional;

import static fr.dossierfacile.common.enums.DocumentCategory.FINANCIAL;
import static fr.dossierfacile.common.enums.DocumentCategory.TAX;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SALARY;

public record GuessedDocumentCategory(
        DocumentCategory category,
        DocumentSubCategory subCategory
) {

    public static Optional<GuessedDocumentCategory> forFile(InMemoryPdfFile pdfFile, BarCodeDocumentType documentType) {
        var guess = switch (documentType) {
            case MON_FRANCE_CONNECT -> MonFranceConnectDocumentType.of(pdfFile).getCategory().orElse(null);
            case PAYFIT_PAYSLIP, SNCF_PAYSLIP -> new GuessedDocumentCategory(FINANCIAL, SALARY);
            case TAX_ASSESSMENT -> new GuessedDocumentCategory(TAX, MY_NAME);
            case UNKNOWN -> null;
        };
        return Optional.ofNullable(guess);
    }

    public boolean isMatchingCategoryOf(Document document) {
        return document.getDocumentCategory() == category &&
                document.getDocumentSubCategory() == subCategory;
    }

}
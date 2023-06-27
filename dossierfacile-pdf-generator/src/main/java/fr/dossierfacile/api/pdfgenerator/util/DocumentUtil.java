package fr.dossierfacile.api.pdfgenerator.util;

import fr.dossierfacile.common.entity.BarCodeType;
import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;

import java.util.Objects;

public class DocumentUtil {

    public static boolean hasValid2DDocOnTaxDocument(Tenant tenant) {
        return tenant.getDocuments().stream()
                .filter(document -> document.getDocumentCategory() == DocumentCategory.TAX)
                .anyMatch(taxDocument -> taxDocument.getFiles().stream()
                        .map(File::getFileAnalysis)
                        .filter(Objects::nonNull)
                        .anyMatch(analysis -> analysis.getBarCodeType() == BarCodeType.TWO_D_DOC
                                && analysis.getDocumentType() == BarCodeDocumentType.TAX_ASSESSMENT
                                && analysis.getAuthenticationStatus() == FileAuthenticationStatus.VALID)
                );
    }

}

package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;

import java.util.Objects;

public enum AuthenticityStatus {

    AUTHENTIC,
    UNKNOWN;

    public static AuthenticityStatus isAuthentic(Document document) {
        if (document.getDocumentCategory() == DocumentCategory.TAX) {
            boolean isAuthentic = document.getFiles().stream()
                    .map(File::getFileAnalysis)
                    .filter(Objects::nonNull)
                    .anyMatch(analysis -> analysis.getDocumentType() == BarCodeDocumentType.TAX_ASSESSMENT
                            && analysis.getAuthenticationStatus() == FileAuthenticationStatus.VALID
                            && analysis.getBarCodeType() == BarCodeType.TWO_D_DOC);

            return fromBoolean(isAuthentic);
        }
        return AuthenticityStatus.UNKNOWN;
    }

    private static AuthenticityStatus fromBoolean(boolean isAuthentic) {
        return isAuthentic ? AUTHENTIC : UNKNOWN;
    }

}

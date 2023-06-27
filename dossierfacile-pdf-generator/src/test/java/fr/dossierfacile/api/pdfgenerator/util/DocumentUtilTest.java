package fr.dossierfacile.api.pdfgenerator.util;

import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.BarCodeType;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static fr.dossierfacile.api.pdfgenerator.util.DocumentUtil.hasValid2DDocOnTaxDocument;
import static fr.dossierfacile.common.enums.DocumentCategory.FINANCIAL;
import static fr.dossierfacile.common.enums.DocumentCategory.IDENTIFICATION;
import static fr.dossierfacile.common.enums.DocumentCategory.PROFESSIONAL;
import static fr.dossierfacile.common.enums.DocumentCategory.RESIDENCY;
import static fr.dossierfacile.common.enums.DocumentCategory.TAX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DocumentUtilTest {

    @Test
    void only_check_tax_category() {
        Tenant tenant = Tenant.builder()
                .documents(documentsWithCategory(IDENTIFICATION, RESIDENCY, PROFESSIONAL, FINANCIAL))
                .build();
        assertFalse(hasValid2DDocOnTaxDocument(tenant));
    }

    @Test
    void only_check_tax_documents_with_files() {
        Tenant tenant = Tenant.builder()
                .documents(documentsWithCategory(IDENTIFICATION, TAX))
                .build();
        assertFalse(hasValid2DDocOnTaxDocument(tenant));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            TWO_D_DOC, TAX_ASSESSMENT, VALID, true
            TWO_D_DOC, TAX_ASSESSMENT, INVALID, false
            TWO_D_DOC, UNKNOWN, VALID, false
            QR_CODE, TAX_ASSESSMENT, VALID, false
            """)
    void verify_bar_code_type_and_issuer_and_status(BarCodeType barCodeType, BarCodeDocumentType documentType, FileAuthenticationStatus status, boolean expected) {
        Tenant tenant = tenantWithTaxAnalysis(fileAnalysis(barCodeType, status, documentType));
        assertThat(hasValid2DDocOnTaxDocument(tenant)).isEqualTo(expected);
    }

    private Tenant tenantWithTaxAnalysis(BarCodeFileAnalysis fileAnalysis) {
        return Tenant.builder()
                .documents(List.of(Document.builder()
                        .documentCategory(TAX)
                        .files(List.of(File.builder()
                                .fileAnalysis(fileAnalysis)
                                .build()))
                        .build()))
                .build();
    }

    private static BarCodeFileAnalysis fileAnalysis(BarCodeType barCodeType, FileAuthenticationStatus fileAuthenticationStatus, BarCodeDocumentType documentType) {
        return BarCodeFileAnalysis.builder()
                .barCodeType(barCodeType)
                .authenticationStatus(fileAuthenticationStatus)
                .documentType(documentType)
                .build();
    }

    private static List<Document> documentsWithCategory(DocumentCategory... categories) {
        return Arrays.stream(categories)
                .map(category -> Document.builder()
                        .documentCategory(category)
                        .build())
                .toList();
    }

}
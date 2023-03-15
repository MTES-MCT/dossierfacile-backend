package fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.service.qrcodeanalysis.GuessedDocumentCategory;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static fr.dossierfacile.process.file.TestFilesUtil.getPdfFile;
import static org.assertj.core.api.Assertions.assertThat;

class MonFranceConnectDocumentTypeTest {

    private InMemoryPdfFile file;

    @ParameterizedTest
    @CsvSource({
            "monfranceconnect/tax-document.pdf, TAXABLE_INCOME",
            "monfranceconnect/student-document.pdf, STUDENT_STATUS",
            "monfranceconnect/scholarship-document.pdf, SCHOLARSHIP",
            "monfranceconnect/unemployment-document.pdf, UNEMPLOYMENT_STATUS",
            "monfranceconnect/unemployment-benefit-document.pdf, UNEMPLOYMENT_BENEFIT",
            "test-document.pdf, UNKNOWN"
    })
    void should_guess_document_type_based_on_title(String fileName, MonFranceConnectDocumentType expectedType) throws IOException {
        file = getPdfFile(fileName);
        assertThat(MonFranceConnectDocumentType.of(file)).isEqualTo(expectedType);
    }

    @ParameterizedTest
    @CsvSource({
            "tax-document.pdf, TAX, MY_NAME",
            "student-document.pdf, PROFESSIONAL, STUDENT",
            "scholarship-document.pdf, FINANCIAL, SCHOLARSHIP",
            "unemployment-document.pdf, PROFESSIONAL, UNEMPLOYED",
            "unemployment-benefit-document.pdf, FINANCIAL, SOCIAL_SERVICE",
    })
    void should_guess_document_category(String fileName, DocumentCategory category, DocumentSubCategory subCategory) throws IOException {
        file = getPdfFile("monfranceconnect/" + fileName);
        GuessedDocumentCategory expectedCategory = new GuessedDocumentCategory(category, subCategory);

        assertThat(MonFranceConnectDocumentType.of(file).getCategory())
                .isPresent()
                .contains(expectedCategory);
    }

    @AfterEach
    void tearDown() throws IOException {
        file.close();
    }

}
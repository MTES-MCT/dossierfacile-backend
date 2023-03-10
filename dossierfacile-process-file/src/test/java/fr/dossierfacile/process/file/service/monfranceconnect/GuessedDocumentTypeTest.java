package fr.dossierfacile.process.file.service.monfranceconnect;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static fr.dossierfacile.process.file.TestFilesUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

class GuessedDocumentTypeTest {

    private InMemoryPdfFile file;

    @ParameterizedTest
    @CsvSource({
            "monfranceconnect/tax-document.pdf, TAX",
            "monfranceconnect/scholarship-document.pdf, SCHOLARSHIP",
            "test-document.pdf, UNKNOWN"
    })
    void should_guess_document_type_based_on_title(String fileName, GuessedDocumentType expectedType) throws IOException {
        file = getPdfFile(fileName);
        assertThat(GuessedDocumentType.of(file)).isEqualTo(expectedType);
    }

    @ParameterizedTest
    @CsvSource({
        "tax-document.pdf, TAX, MY_NAME",
        "scholarship-document.pdf, FINANCIAL, SCHOLARSHIP",
    })
    void should_match_with_the_actual_category(String fileName, DocumentCategory category, DocumentSubCategory subCategory) throws IOException {
        file = getPdfFile("monfranceconnect/" + fileName);
        GuessedDocumentType guessedDocumentType = GuessedDocumentType.of(file);
        Document actualDocument = documentWith(category, subCategory);

        assertThat(guessedDocumentType.isMatchingCategoryOf(actualDocument)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "tax-document.pdf, FINANCIAL, MY_NAME",
        "scholarship-document.pdf, FINANCIAL, SALARY",
    })
    void should_not_match_with_the_actual_category(String fileName, DocumentCategory category, DocumentSubCategory subCategory) throws IOException {
        file = getPdfFile("monfranceconnect/" + fileName);
        GuessedDocumentType guessedDocumentType = GuessedDocumentType.of(file);
        Document actualDocument = documentWith(category, subCategory);

        assertThat(guessedDocumentType.isMatchingCategoryOf(actualDocument)).isFalse();
    }

    @AfterEach
    void tearDown() throws IOException {
        file.close();
    }

    private static Document documentWith(DocumentCategory category, DocumentSubCategory subCategory) {
        return Document.builder()
                .documentCategory(category)
                .documentSubCategory(subCategory)
                .build();
    }

}
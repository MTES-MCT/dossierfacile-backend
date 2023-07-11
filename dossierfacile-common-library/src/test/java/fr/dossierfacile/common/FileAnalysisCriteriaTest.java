package fr.dossierfacile.common;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static fr.dossierfacile.common.enums.DocumentCategory.TAX;
import static fr.dossierfacile.common.enums.DocumentSubCategory.*;
import static org.assertj.core.api.Assertions.assertThat;

class FileAnalysisCriteriaTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
            TAX, MY_NAME
            PROFESSIONAL, CDI
            PROFESSIONAL, STUDENT
            PROFESSIONAL, UNEMPLOYED
            FINANCIAL, SALARY
            FINANCIAL, SCHOLARSHIP
            FINANCIAL, SOCIAL_SERVICE
            RESIDENCY, GUEST
            """)
    void should_analyze_files_in_categories(DocumentCategory documentCategory, DocumentSubCategory documentSubCategory) {
        File file = fileInCategory(documentCategory, documentSubCategory, false);

        assertThat(FileAnalysisCriteria.shouldBeAnalyzed(file)).isTrue();
    }

    @Test
    void should_not_analyze_document_with_no_files() {
        File file = fileInCategory(TAX, MY_PARENTS, true);

        assertThat(FileAnalysisCriteria.shouldBeAnalyzed(file)).isFalse();
    }

    private static File fileInCategory(DocumentCategory documentCategory, DocumentSubCategory documentSubCategory, boolean noDocument) {
        return File.builder()
                .document(Document.builder()
                        .tenant(Tenant.builder().status(TenantFileStatus.TO_PROCESS).build())
                        .documentCategory(documentCategory)
                        .documentSubCategory(documentSubCategory)
                        .noDocument(noDocument)
                        .build())
                .build();
    }

}
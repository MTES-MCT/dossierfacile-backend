package fr.dossierfacile.common;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class FileAnalysisCriteriaTest {

    @ParameterizedTest
    @CsvSource({"VALIDATED", "ARCHIVED"})
    void should_not_analyze_files_of_validated_or_archived_tenants(TenantFileStatus status) {
        File file = taxFileWithTenantStatus(status);

        assertThat(FileAnalysisCriteria.shouldBeAnalyzed(file)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"TO_PROCESS","DECLINED", "INCOMPLETE"})
    void should_analyze_other_files(TenantFileStatus status) {
        File file = taxFileWithTenantStatus(status);

        assertThat(FileAnalysisCriteria.shouldBeAnalyzed(file)).isTrue();
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            TAX, MY_NAME
            PROFESSIONAL, STUDENT
            PROFESSIONAL, UNEMPLOYED
            FINANCIAL, SALARY
            FINANCIAL, SCHOLARSHIP
            FINANCIAL, SOCIAL_SERVICE
            """)
    void should_analyze_files_in_categories(DocumentCategory documentCategory, DocumentSubCategory documentSubCategory) {
        File file = fileInCategory(documentCategory, documentSubCategory);

        assertThat(FileAnalysisCriteria.shouldBeAnalyzed(file)).isTrue();
    }

    private static File taxFileWithTenantStatus(TenantFileStatus status) {
        return File.builder()
                .document(Document.builder()
                        .tenant(Tenant.builder().status(status).build())
                        .documentCategory(DocumentCategory.TAX)
                        .documentSubCategory(DocumentSubCategory.MY_NAME)
                        .build())
                .build();
    }

    private static File fileInCategory(DocumentCategory documentCategory, DocumentSubCategory documentSubCategory) {
        return File.builder()
                .document(Document.builder()
                        .tenant(Tenant.builder().status(TenantFileStatus.TO_PROCESS).build())
                        .documentCategory(documentCategory)
                        .documentSubCategory(documentSubCategory)
                        .build())
                .build();
    }

}
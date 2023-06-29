package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.model.apartment_sharing.DocumentModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static fr.dossierfacile.common.enums.DocumentSubCategory.DRIVERS_LICENSE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_IDENTIFICATION;
import static org.assertj.core.api.Assertions.assertThat;

interface DocumentMappingTest {

    DocumentModel mapDocument(Document document);

    @ParameterizedTest
    @EnumSource(value = DocumentSubCategory.class,
            names = {"INTERMITTENT", "STAY_AT_HOME_PARENT", "NO_ACTIVITY", "ARTIST"})
    @DisplayName("New categories are replaced by 'OTHER' in field 'documentSubCategory'")
    default void should_replace_new_professional_categories_by_other(DocumentSubCategory subCategory) {
        Document document = Document.builder()
                .documentSubCategory(subCategory)
                .build();

        DocumentModel documentModel = mapDocument(document);

        assertThat(documentModel.getDocumentSubCategory()).isEqualTo(OTHER);
    }

    @Test
    @DisplayName("Driver's license is replaced by 'OTHER_IDENTIFICATION' in field 'documentSubCategory'")
    default void should_replace_drivers_license_by_other() {
        Document document = Document.builder()
                .documentSubCategory(DRIVERS_LICENSE)
                .build();

        DocumentModel documentModel = mapDocument(document);

        assertThat(documentModel.getDocumentSubCategory()).isEqualTo(OTHER_IDENTIFICATION);
    }

    @ParameterizedTest
    @EnumSource(value = DocumentSubCategory.class)
    @DisplayName("All categories are mapped as is in field 'subCategory'")
    default void should_expose_all_categories(DocumentSubCategory subCategory) {
        Document document = Document.builder()
                .documentSubCategory(subCategory)
                .build();

        DocumentModel documentModel = mapDocument(document);

        assertThat(documentModel.getSubCategory()).isEqualTo(subCategory);
    }

}
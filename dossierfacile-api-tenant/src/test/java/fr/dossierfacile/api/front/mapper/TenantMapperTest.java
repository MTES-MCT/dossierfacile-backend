package fr.dossierfacile.api.front.mapper;

import fr.dossierfacile.api.front.model.tenant.DocumentModel;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER;
import static org.assertj.core.api.Assertions.assertThat;

class TenantMapperTest {

    private final TenantMapper mapper = new TenantMapperImpl();

    @ParameterizedTest
    @EnumSource(value = DocumentSubCategory.class,
            names = {"INTERMITTENT", "STAY_AT_HOME_PARENT", "NO_ACTIVITY", "ARTIST"})
    @DisplayName("New categories are replaced by 'OTHER' in field 'documentSubCategory'")
    void should_replace_new_categories_by_other(DocumentSubCategory subCategory) {
        Document document = Document.builder()
                .documentSubCategory(subCategory)
                .build();

        DocumentModel documentModel = mapper.toDocumentModel(document);

        assertThat(documentModel.getDocumentSubCategory()).isEqualTo(OTHER);
    }

    @ParameterizedTest
    @EnumSource(value = DocumentSubCategory.class)
    @DisplayName("All categories are mapped as is in field 'subCategory'")
    void should_expose_all_categories(DocumentSubCategory subCategory) {
        Document document = Document.builder()
                .documentSubCategory(subCategory)
                .build();

        DocumentModel documentModel = mapper.toDocumentModel(document);

        assertThat(documentModel.getSubCategory()).isEqualTo(subCategory);
    }

}
package fr.dossierfacile.document.analysis.rule.validator.mapper;

import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.model.document_ia.ExtractionModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.document.analysis.rule.validator.mapper.model.TestModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentIASpecificMapperTest {

    @Nested
    class TestModelMapperTest {

        private List<GenericProperty> makeInnerModel() {
            return List.of(
                    GenericProperty.builder().name("inner_string").value("inner_value").type("string").build(),
                    GenericProperty.builder().name("inner_date").value("2026-06-04").type("date").build(),
                    GenericProperty.builder().name("inner_list").value(List.of("value4", "value5", "value6")).type("list").build()
            );
        }

        private ExtractionModel makeSimpleTestModel() {

            List<GenericProperty> properties = List.of(
                    GenericProperty.builder().name("string_value").value("1111111111").type("string").build(),
                    GenericProperty.builder().name("date_value").value("2026-03-02").type("date").build(),
                    GenericProperty.builder().name("list").value(List.of("value1", "value2", "value3")).type("list").build(),
                    GenericProperty.builder().name("inner_model").value(makeInnerModel()).type("object").build()
            );

            return ExtractionModel.builder()
                    .type("test")
                    .properties(properties)
                    .build();
        }

        private ResultModel getTestModelResultModel(ExtractionModel extractionModel) {
            return ResultModel.builder()
                    .extraction(extractionModel)
                    .barcodes(List.of())
                    .build();
        }

        private DocumentIAFileAnalysis getTestModelDocumentIaAnalysis(ResultModel resultModel) {
            return DocumentIAFileAnalysis.builder()
                    .result(resultModel)
                    .build();
        }

        @Test
        void testMergerMapper() {
            var mapper = new DocumentIAMergerMapper();
            var analysis = List.of(getTestModelDocumentIaAnalysis(getTestModelResultModel(makeSimpleTestModel())));
            var testModel = mapper.map(analysis, TestModel.class);

            assertThat(testModel).isNotNull();
            assertThat(testModel.isPresent()).isTrue();
            assertThat(testModel.get().getStringValue()).isEqualTo("1111111111");
            assertThat(testModel.get().getDateValue()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(testModel.get().getList()).hasSize(3).containsExactly("value1", "value2", "value3");

            assertThat(testModel.get().getTestInnerModel()).isNotNull();
            assertThat(testModel.get().getTestInnerModel().getInnerString()).isEqualTo("inner_value");
            assertThat(testModel.get().getTestInnerModel().getInnerDate()).isEqualTo(LocalDate.of(2026, 6, 4));
            assertThat(testModel.get().getTestInnerModel().getInnerList())
                    .hasSize(3)
                    .containsExactly("value4", "value5", "value6");
        }


    }

}

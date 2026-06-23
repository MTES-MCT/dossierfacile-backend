package fr.dossierfacile.document.analysis.rule.validator.mapper;

import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.model.document_ia.ExtractionModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAMergerMapper;
import fr.dossierfacile.document.analysis.rule.validator.mapper.model.TestModel;
import fr.dossierfacile.document.analysis.rule.validator.property_tax.document_ia_model.PropertyTaxModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentIASpecificMapperTest {

    @Nested
    class TestModelMapperTest {

        private GenericProperty getListItem(String testSuffix) {
            return GenericProperty.builder().name("item").value(makeInnerModel(testSuffix)).type("object").build();
        }

        private List<GenericProperty> makeInnerModel(String testSuffix) {
            return List.of(
                    GenericProperty.builder().name("inner_string").value(testSuffix + "inner_value").type("string").build(),
                    GenericProperty.builder().name("inner_date").value("2026-06-04").type("date").build(),
                    GenericProperty.builder().name("inner_list").value(List.of(testSuffix + "value4", testSuffix + "value5", testSuffix +"value6")).type("list").build()
            );
        }

        private ExtractionModel makeSimpleTestModel() {

            List<GenericProperty> properties = List.of(
                    GenericProperty.builder().name("string_value").value("1111111111").type("string").build(),
                    GenericProperty.builder().name("date_value").value("2026-03-02").type("date").build(),
                    GenericProperty.builder().name("list").value(List.of("value1", "value2", "value3")).type("list").build(),
                    GenericProperty.builder().name("inner_model").value(makeInnerModel("inner_model_")).type("object").build(),
                    GenericProperty.builder().name("list_inner_model").value(
                            List.of(
                                getListItem("1_"),
                                getListItem("2_"),
                                getListItem("3_")
                            )
                    ).type("list").build()
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

            assertThat(testModel).isNotNull().isPresent();
            assertThat(testModel.get().getStringValue()).isEqualTo("1111111111");
            assertThat(testModel.get().getDateValue()).isEqualTo(LocalDate.of(2026, Month.MARCH, 2));
            assertThat(testModel.get().getList()).hasSize(3).containsExactly("value1", "value2", "value3");

            assertThat(testModel.get().getTestInnerModel()).isNotNull();
            assertThat(testModel.get().getTestInnerModel().getInnerString()).isEqualTo("inner_model_inner_value");
            assertThat(testModel.get().getTestInnerModel().getInnerDate()).isEqualTo(LocalDate.of(2026, Month.JUNE, 4));
            assertThat(testModel.get().getTestInnerModel().getInnerList())
                    .hasSize(3)
                    .containsExactly("inner_model_value4", "inner_model_value5", "inner_model_value6");

            assertThat(testModel.get().getListTestInnerModels()).hasSize(3);
            assertThat(testModel.get().getListTestInnerModels().get(0).getInnerString()).isEqualTo("1_inner_value");
            assertThat(testModel.get().getListTestInnerModels().get(1).getInnerString()).isEqualTo("2_inner_value");
            assertThat(testModel.get().getListTestInnerModels().get(2).getInnerString()).isEqualTo("3_inner_value");
        }


    }

    @Nested
    class TaxeFonciereModelMapperTest {

        private DocumentIAFileAnalysis analysisWithExtraction(List<GenericProperty> properties) {
            ResultModel resultModel = ResultModel.builder()
                    .extraction(ExtractionModel.builder().type("taxe_fonciere").properties(properties).build())
                    .barcodes(List.of())
                    .build();
            return DocumentIAFileAnalysis.builder().result(resultModel).build();
        }

        @Test
        void should_map_owner_identity_and_year_from_extraction() {
            var mapper = new DocumentIAMergerMapper();
            var analysis = List.of(analysisWithExtraction(List.of(
                    GenericProperty.builder().name("annee_imposition").value("2025").type("string").build(),
                    GenericProperty.builder().name("proprietaire_identite").value("DUPONT Camille").type("string").build(),
                    GenericProperty.builder().name("adresse_bien_impose").value("10 RUE DE LA PAIX").type("string").build()
            )));

            var model = mapper.map(analysis, PropertyTaxModel.class);

            assertThat(model).isPresent();
            assertThat(model.get().anneeImposition).isEqualTo("2025");
            assertThat(model.get().proprietaireIdentite).isEqualTo("DUPONT Camille");
        }
    }

}

package fr.dossierfacile.api.front.service.document.analysis.rule.validator.mapper;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.DocumentIAMultiMapper;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.PropertyTransformer;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.documentIA.BarcodeModel;
import fr.dossierfacile.common.model.documentIA.ExtractionModel;
import fr.dossierfacile.common.model.documentIA.GenericProperty;
import fr.dossierfacile.common.model.documentIA.ResultModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentIAMultiMapperTest {

    public static class TestTargetModel {

        public static class TestTransformer implements PropertyTransformer<String, String> {
            @Override
            public String transform(String input) {
                return "TEST_TRANSFORMED_" + input;
            }
        }

        @DocumentIAField(twoDDocName = "nom_patronymique", extractionName = "nom")
        String lastName;

        @DocumentIAField(twoDDocName = "prenoms", extractionName = "prenom")
        String firstNames;

        @DocumentIAField(twoDDocName = "date_naissance", extractionName = "date_naissance", type = DocumentIAPropertyType.DATE)
        LocalDate birthDate;

        @DocumentIAField(twoDDocName = "numero_document", extractionName = "numero_document")
        String documentNumber;

        @DocumentIAField(twoDDocName = "test_transformer", extractionName = "test_transformer", type = DocumentIAPropertyType.STRING, transformer = TestTransformer.class)
        String transformedField;
    }

    // ==========================
    // Fixtures helpers
    // ==========================

    private BarcodeModel generateBarcodeModel(GenericProperty... properties) {
        return BarcodeModel.builder()
                .type("DATA_MATRIX")
                .typedData(Stream.of(properties).filter(Objects::nonNull).toList())
                .build();
    }

    private DocumentIAFileAnalysis analysisWith2DDoc(BarcodeModel... barcodes) {
        ResultModel result = ResultModel.builder()
                .barcodes(Stream.of(barcodes).filter(Objects::nonNull).toList())
                .extraction(null)
                .build();
        return DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }

    private DocumentIAFileAnalysis analysisWithExtraction(GenericProperty... properties) {
        ExtractionModel extraction = ExtractionModel.builder()
                .type("cni")
                .properties(Stream.of(properties).filter(Objects::nonNull).toList())
                .build();
        ResultModel result = ResultModel.builder()
                .extraction(extraction)
                .barcodes(List.of())
                .build();
        return DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }

    private DocumentIAFileAnalysis analysisWithBoth(BarcodeModel[] barcodes, GenericProperty[] extractionProperties) {
        ExtractionModel extraction = ExtractionModel.builder()
                .type("cni")
                .properties(Stream.of(extractionProperties).filter(Objects::nonNull).toList())
                .build();
        ResultModel result = ResultModel.builder()
                .extraction(extraction)
                .barcodes(Stream.of(barcodes).filter(Objects::nonNull).toList())
                .build();
        return DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }

    private GenericProperty stringProp(String name, String value) {
        return GenericProperty.builder().name(name).type("string").value(value).build();
    }

    private GenericProperty dateProp(String name, LocalDate date) {
        return GenericProperty.builder().name(name).type("date").value(date != null ? date.toString() : null).build();
    }

    // ==========================
    // Tests
    // ==========================

    @Test
    @DisplayName("Retourne une liste vide quand aucune analyse n'est fournie")
    void should_return_empty_list_when_no_analysis() {
        List<TestTargetModel> results = new DocumentIAMultiMapper().map(List.of(), TestTargetModel.class);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Retourne un objet mappé depuis une seule analyse avec 2DDoc")
    void should_map_single_analysis_with_2ddoc() {
        var analysis = analysisWith2DDoc(
                generateBarcodeModel(
                        stringProp("nom_patronymique", "Dupont"),
                        stringProp("prenoms", "Jean Pierre"),
                        dateProp("date_naissance", LocalDate.of(1990, 5, 20)),
                        stringProp("numero_document", "AB123456"),
                        stringProp("test_transformer", "original_value")
                )
        );

        List<TestTargetModel> results = new DocumentIAMultiMapper().map(List.of(analysis), TestTargetModel.class);

        assertThat(results).hasSize(1);
        TestTargetModel result = results.getFirst();
        assertThat(result.lastName).isEqualTo("Dupont");
        assertThat(result.firstNames).isEqualTo("Jean Pierre");
        assertThat(result.birthDate).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(result.documentNumber).isEqualTo("AB123456");
        assertThat(result.transformedField).isEqualTo("TEST_TRANSFORMED_original_value");
    }

    @Test
    @DisplayName("Retourne un objet mappé depuis une seule analyse avec Extraction")
    void should_map_single_analysis_with_extraction() {
        var analysis = analysisWithExtraction(
                stringProp("nom", "Martin"),
                stringProp("prenom", "Paul"),
                dateProp("date_naissance", LocalDate.of(1985, 3, 10))
        );

        List<TestTargetModel> results = new DocumentIAMultiMapper().map(List.of(analysis), TestTargetModel.class);

        assertThat(results).hasSize(1);
        TestTargetModel result = results.getFirst();
        assertThat(result.lastName).isEqualTo("Martin");
        assertThat(result.firstNames).isEqualTo("Paul");
        assertThat(result.birthDate).isEqualTo(LocalDate.of(1985, 3, 10));
    }

    @Test
    @DisplayName("Priorise les données 2DDoc sur l'extraction au sein d'une même analyse")
    void should_prioritize_2ddoc_over_extraction() {
        var analysis = analysisWithBoth(
                new BarcodeModel[]{generateBarcodeModel(stringProp("nom_patronymique", "Dupont"))},
                new GenericProperty[]{stringProp("nom", "Martin")}
        );

        List<TestTargetModel> results = new DocumentIAMultiMapper().map(List.of(analysis), TestTargetModel.class);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().lastName).isEqualTo("Dupont");
    }

    @Test
    @DisplayName("Mappe plusieurs analyses en une liste de plusieurs résultats")
    void should_map_multiple_analyses() {
        var analysis1 = analysisWith2DDoc(generateBarcodeModel(
                stringProp("nom_patronymique", "Dupont"),
                stringProp("prenoms", "Jean")
        ));
        var analysis2 = analysisWithExtraction(
                stringProp("nom", "Martin"),
                stringProp("prenom", "Paul")
        );

        List<TestTargetModel> results = new DocumentIAMultiMapper().map(List.of(analysis1, analysis2), TestTargetModel.class);

        assertThat(results).hasSize(2);

        TestTargetModel res1 = results.getFirst(); // L'ordre de la liste d'entrée doit être préservé
        assertThat(res1.lastName).isEqualTo("Dupont");
        assertThat(res1.firstNames).isEqualTo("Jean");

        TestTargetModel res2 = results.get(1);
        assertThat(res2.lastName).isEqualTo("Martin");
        assertThat(res2.firstNames).isEqualTo("Paul");
    }

    @Test
    @DisplayName("Ignore les analyses qui ne produisent aucun résultat mappé")
    void should_ignore_analysis_without_mappable_data() {
        var analysis1 = analysisWith2DDoc(generateBarcodeModel(
                stringProp("nom_patronymique", "Dupont")
        ));
        var emptyAnalysis = analysisWithExtraction(); // Pas de propriétés

        List<TestTargetModel> results = new DocumentIAMultiMapper().map(List.of(analysis1, emptyAnalysis), TestTargetModel.class);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().lastName).isEqualTo("Dupont");
    }
}
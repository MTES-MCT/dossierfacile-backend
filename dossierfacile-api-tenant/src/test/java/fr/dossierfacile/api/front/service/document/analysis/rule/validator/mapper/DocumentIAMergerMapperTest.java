package fr.dossierfacile.api.front.service.document.analysis.rule.validator.mapper;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.DocumentIAMergerMapper;
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
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentIAMergerMapperTest {

    // ==========================
    // Modèle cible pour les tests
    // ==========================

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

    public static class TestTargetModelOnlyExtraction {
        @DocumentIAField(extractionName = "nom")
        String lastName;

        @DocumentIAField(extractionName = "prenom")
        String firstNames;
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

    private DocumentIAFileAnalysis buildFullAnalysis(BarcodeModel[] barcodes, GenericProperty[] extractionProperties) {
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
        return GenericProperty.builder()
                .name(name)
                .type("string")
                .value(value)
                .build();
    }

    private GenericProperty dateProp(String name, LocalDate date) {
        return GenericProperty.builder()
                .name(name)
                .type("date")
                .value(date != null ? date.toString() : null)
                .build();
    }

    // ==========================
    // Tests
    // ==========================

    @Test
    @DisplayName("Retourne empty quand aucune propriété n'est mappable")
    void map_returns_empty_when_no_properties() {
        List<DocumentIAFileAnalysis> analyses = List.of(
                analysisWith2DDoc(),
                analysisWithExtraction()
        );

        Optional<TestTargetModel> result = new DocumentIAMergerMapper().map(analyses, TestTargetModel.class);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Mappe correctement les propriétés depuis un 2DDoc complet")
    void map_complet_object_when_2D_doc_analysis() {
        List<DocumentIAFileAnalysis> analyses = List.of(
                analysisWith2DDoc(
                        generateBarcodeModel(
                                stringProp("nom_patronymique", "Dupont"),
                                stringProp("prenoms", "Jean Pierre"),
                                dateProp("date_naissance", LocalDate.of(1990, 5, 20)),
                                stringProp("numero_document", "AB123456"),
                                stringProp("test_transformer", "original_value")
                        )
                )
        );

        Optional<TestTargetModel> result = new DocumentIAMergerMapper().map(analyses, TestTargetModel.class);

        assertThat(result).isPresent();
        assertThat(result.get().lastName).isEqualTo("Dupont");
        assertThat(result.get().firstNames).isEqualTo("Jean Pierre");
        assertThat(result.get().birthDate).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(result.get().documentNumber).isEqualTo("AB123456");
        assertThat(result.get().transformedField).isEqualTo("TEST_TRANSFORMED_original_value");

    }

    @Test
    @DisplayName("Merge les données entre plusieurs 2DDoc de la meme analyse, le dernier 2D DOC a la priorité")
    void map_complet_object_from_multiple_barcodes() {
        List<DocumentIAFileAnalysis> analyses = List.of(
                analysisWith2DDoc(
                        generateBarcodeModel(
                                stringProp("nom_patronymique", "Dupont"),
                                stringProp("prenoms", "Jean Pierre"),
                                stringProp("numero_document", "11111111")
                        ),
                        generateBarcodeModel(
                                dateProp("date_naissance", LocalDate.of(1990, 5, 20)),
                                stringProp("numero_document", "AB123456"),
                                stringProp("test_transformer", "original_value")
                        )
                )
        );

        Optional<TestTargetModel> result = new DocumentIAMergerMapper().map(analyses, TestTargetModel.class);

        assertThat(result).isPresent();
        assertThat(result.get().lastName).isEqualTo("Dupont");
        assertThat(result.get().firstNames).isEqualTo("Jean Pierre");
        assertThat(result.get().birthDate).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(result.get().documentNumber).isEqualTo("AB123456");
        assertThat(result.get().transformedField).isEqualTo("TEST_TRANSFORMED_original_value");

    }

    @Test
    @DisplayName("Merge les données entre plusieurs 2DDoc, la dernière analyse a la priorité")
    void map_complet_object_from_multiple_barcodes_multi_analysis() {
        List<DocumentIAFileAnalysis> analyses = List.of(
                analysisWith2DDoc(
                        generateBarcodeModel(
                                stringProp("nom_patronymique", "Dupont"),
                                stringProp("prenoms", "Jean Pierre"),
                                stringProp("numero_document", "11111111")
                        )
                ),
                analysisWith2DDoc(
                        generateBarcodeModel(
                                dateProp("date_naissance", LocalDate.of(1990, 5, 20)),
                                stringProp("numero_document", "AB123456"),
                                stringProp("test_transformer", "original_value")
                        )
                )
        );

        Optional<TestTargetModel> result = new DocumentIAMergerMapper().map(analyses, TestTargetModel.class);

        assertThat(result).isPresent();
        assertThat(result.get().lastName).isEqualTo("Dupont");
        assertThat(result.get().firstNames).isEqualTo("Jean Pierre");
        assertThat(result.get().birthDate).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(result.get().documentNumber).isEqualTo("AB123456");
        assertThat(result.get().transformedField).isEqualTo("TEST_TRANSFORMED_original_value");

    }

    @Test
    @DisplayName("Mappe correctement les propriétés depuis un 2DDoc et une extraction, le 2DDoc a la priorité")
    void map_complet_object_from_2D_DOC_and_extraction() {
        List<DocumentIAFileAnalysis> analyses = List.of(
                buildFullAnalysis(
                        new BarcodeModel[]{
                                generateBarcodeModel(
                                        stringProp("nom_patronymique", "Dupont"),
                                        stringProp("prenoms", "Jean Pierre")
                                )
                        },
                        new GenericProperty[]{
                                stringProp("nom", "ShouldNotBeUsed"),
                                stringProp("prenom", "ShouldNotBeUsed"),
                                dateProp("date_naissance", LocalDate.of(1990, 5, 20)),
                                stringProp("numero_document", "AB123456"),
                                stringProp("test_transformer", "original_value")
                        }
                )
        );

        Optional<TestTargetModel> result = new DocumentIAMergerMapper().map(analyses, TestTargetModel.class);

        assertThat(result).isPresent();
        assertThat(result.get().lastName).isEqualTo("Dupont");
        assertThat(result.get().firstNames).isEqualTo("Jean Pierre");
        assertThat(result.get().birthDate).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(result.get().documentNumber).isEqualTo("AB123456");
        assertThat(result.get().transformedField).isEqualTo("TEST_TRANSFORMED_original_value");

    }

    @Test
    @DisplayName("Mappe correctement les propriétés depuis un 2DDoc et une extraction, avec plusieurs analyses")
    void map_complet_object_from_2D_DOC_and_extraction_multiple_analysis() {
        List<DocumentIAFileAnalysis> analyses = List.of(
                analysisWith2DDoc(
                        generateBarcodeModel(
                                stringProp("nom_patronymique", "Dupont"),
                                stringProp("prenoms", "Jean Pierre")
                        )
                ),
                analysisWithExtraction(
                        stringProp("nom", "ShouldNotBeUsed"),
                        stringProp("prenom", "ShouldNotBeUsed"),
                        dateProp("date_naissance", LocalDate.of(1990, 5, 20)),
                        stringProp("numero_document", "AB123456"),
                        stringProp("test_transformer", "original_value")
                )
        );

        Optional<TestTargetModel> result = new DocumentIAMergerMapper().map(analyses, TestTargetModel.class);

        assertThat(result).isPresent();
        assertThat(result.get().lastName).isEqualTo("Dupont");
        assertThat(result.get().firstNames).isEqualTo("Jean Pierre");
        assertThat(result.get().birthDate).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(result.get().documentNumber).isEqualTo("AB123456");
        assertThat(result.get().transformedField).isEqualTo("TEST_TRANSFORMED_original_value");

    }

    @Test
    @DisplayName("Mappe correctement les propriétés quand seul extractionName est défini")
    void map_object_with_only_extraction_name() {
        List<DocumentIAFileAnalysis> analyses = List.of(
                analysisWithExtraction(
                        stringProp("nom", "Martin"),
                        stringProp("prenom", "Paul")
                )
        );

        Optional<TestTargetModelOnlyExtraction> result = new DocumentIAMergerMapper().map(analyses, TestTargetModelOnlyExtraction.class);

        assertThat(result).isPresent();
        assertThat(result.get().lastName).isEqualTo("Martin");
        assertThat(result.get().firstNames).isEqualTo("Paul");
    }

}

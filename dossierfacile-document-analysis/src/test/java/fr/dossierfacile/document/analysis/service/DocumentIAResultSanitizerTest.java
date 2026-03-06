package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.ExtractionModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia_model.TestFullDiscriminatorModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia_model.TestIdentityModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia_model.TestIdentityTwoDDocModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia_model.TestOtherIdentityModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia_model.TestSubCategoryDiscriminatorModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentIAResultSanitizerTest {

    @Nested
    class testGetFilteredListOfClass {

        static Stream<Arguments> matchingClassCases() {
            return Stream.of(
                    Arguments.of(
                            Document.builder()
                                    .id(1L)
                                    .documentCategory(DocumentCategory.FINANCIAL)
                                    .documentSubCategory(DocumentSubCategory.CDI)
                                    .documentCategoryStep(DocumentCategoryStep.RENT_ANNUITY_LIFE)
                                    .build(),
                            1,
                            List.of(TestFullDiscriminatorModel.class)
                    ),
                    Arguments.of(
                            Document.builder()
                                    .id(1L)
                                    .documentCategory(DocumentCategory.FINANCIAL)
                                    .documentSubCategory(DocumentSubCategory.CDI)
                                    .documentCategoryStep(DocumentCategoryStep.RENT_OTHER)
                                    .build(),
                            0,
                            List.of()
                    ),
                    Arguments.of(
                            Document.builder()
                                    .id(1L)
                                    .documentCategory(DocumentCategory.TAX)
                                    .documentSubCategory(DocumentSubCategory.OTHER_TAX)
                                    .documentCategoryStep(DocumentCategoryStep.TAX_FRENCH_NOTICE)
                                    .build(),
                            0,
                            List.of()
                    ),
                    Arguments.of(
                            Document.builder()
                                    .id(1L)
                                    .documentCategory(DocumentCategory.TAX)
                                    .documentSubCategory(DocumentSubCategory.MY_NAME)
                                    .documentCategoryStep(DocumentCategoryStep.TAX_FRENCH_NOTICE)
                                    .build(),
                            1,
                            List.of(TestSubCategoryDiscriminatorModel.class)
                    )
            );
        }

        @ParameterizedTest
        @MethodSource("matchingClassCases")
        void should_return_only_corresponding_class(Document document, int expectedSize, List<Class<?>> expectedClasses) {
            DocumentIAResultSanitizer sanitizer = new DocumentIAResultSanitizer();
            sanitizer.setDocumentIaModelClassesOverride(List.of(TestIdentityModel.class, TestFullDiscriminatorModel.class, TestSubCategoryDiscriminatorModel.class));
            sanitizer.init();

            var listOfClass = sanitizer.getFilteredListOfClass(document);

            assertThat(listOfClass).containsExactlyElementsOf(expectedClasses).size().isEqualTo(expectedSize);
        }
    }

    @Nested
    class testSanitize {

        static Stream<Arguments> sanitizeCases() {
            return Stream.of(
                    Arguments.of(
                            List.of(TestIdentityModel.class),
                            false,
                            List.of("nom_test", "prenom_test")
                    ),
                    Arguments.of(
                            List.of(TestIdentityModel.class, TestOtherIdentityModel.class),
                            true,
                            List.of("nom_test", "prenom_test", "expiration")
                    )
            );
        }

        @ParameterizedTest
        @MethodSource("sanitizeCases")
        void should_filter_extraction_properties(List<Class<?>> modelClasses, boolean includeExpiration, List<String> expectedNames) {
            DocumentIAResultSanitizer sanitizer = new DocumentIAResultSanitizer();
            sanitizer.setDocumentIaModelClassesOverride(modelClasses);
            sanitizer.init();

            Document document = new Document();
            document.setDocumentCategory(DocumentCategory.IDENTIFICATION);
            document.setDocumentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD);

            ResultModel input = ResultModel.builder()
                    .extraction(ExtractionModel.builder()
                            .properties(buildProperties(includeExpiration))
                            .build())
                    .build();

            ResultModel output = sanitizer.sanitize(input, document);

            assertThat(output.getExtraction().getProperties())
                    .extracting(GenericProperty::getName)
                    .containsExactlyInAnyOrderElementsOf(expectedNames);
        }

        private List<GenericProperty> buildProperties(boolean includeExpiration) {
            List<GenericProperty> properties = List.of(
                    GenericProperty.builder()
                            .name("nom_test")
                            .type(GenericProperty.TYPE_STRING)
                            .value("Dupont")
                            .build(),
                    GenericProperty.builder()
                            .name("prenom_test")
                            .type(GenericProperty.TYPE_STRING)
                            .value("Jean")
                            .build(),
                    GenericProperty.builder()
                            .name("noise")
                            .type(GenericProperty.TYPE_STRING)
                            .value("remove")
                            .build()
            );
            if (!includeExpiration) {
                return properties;
            }
            return List.of(
                    properties.get(0),
                    properties.get(1),
                    GenericProperty.builder()
                            .name("expiration")
                            .type(GenericProperty.TYPE_STRING)
                            .value("test")
                            .build(),
                    properties.get(2)
            );
        }

        @Test
        void should_filter_2d_doc_typed_data_and_remove_unknown_raw_data() {
            DocumentIAResultSanitizer sanitizer = new DocumentIAResultSanitizer();
            sanitizer.setDocumentIaModelClassesOverride(List.of(TestIdentityTwoDDocModel.class));
            sanitizer.init();

            Document document = new Document();
            document.setDocumentCategory(DocumentCategory.IDENTIFICATION);
            document.setDocumentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD);

            BarcodeModel knownTypeBarcode = BarcodeModel.builder()
                    .type("2D_DOC")
                    .antsType("avis_imposition")
                    .rawData(java.util.Map.of("raw_key", "keep"))
                    .typedData(List.of(
                            GenericProperty.builder().name("doc_type").type(GenericProperty.TYPE_STRING).value("28").build(),
                            GenericProperty.builder().name("reference_avis").type(GenericProperty.TYPE_STRING).value("ABC").build(),
                            GenericProperty.builder().name("noise").type(GenericProperty.TYPE_STRING).value("remove").build()
                    ))
                    .build();

            BarcodeModel unknownTypeBarcode = BarcodeModel.builder()
                    .type("2D_DOC")
                    .antsType("other_type")
                    .rawData(java.util.Map.of("raw_key", "remove"))
                    .build();

            BarcodeModel qrcodeBarcode = BarcodeModel.builder()
                    .type("QR")
                    .rawData("http://test.fr")
                    .build();

            ResultModel input = ResultModel.builder()
                    .extraction(ExtractionModel.builder()
                            .properties(List.of(
                                    GenericProperty.builder().name("nom_test").type(GenericProperty.TYPE_STRING).value("Dupont").build(),
                                    GenericProperty.builder().name("noise").type(GenericProperty.TYPE_STRING).value("remove").build()
                            ))
                            .build())
                    .barcodes(List.of(knownTypeBarcode, unknownTypeBarcode, qrcodeBarcode))
                    .build();

            ResultModel output = sanitizer.sanitize(input, document);

            BarcodeModel outputKnown = output.getBarcodes().get(0);
            BarcodeModel outputUnknown = output.getBarcodes().get(1);
            BarcodeModel outputQrcode = output.getBarcodes().get(2);

            assertThat(outputKnown.getTypedData())
                    .extracting(GenericProperty::getName)
                    .containsExactlyInAnyOrder("doc_type", "reference_avis");
            assertThat(outputKnown.getRawData()).isNull();

            assertThat(outputUnknown.getRawData()).isNull();
            assertThat(outputQrcode.getRawData()).isNull();
        }

        @Test
        void should_sanitize_barcodes_even_when_extraction_is_null() {
            DocumentIAResultSanitizer sanitizer = new DocumentIAResultSanitizer();
            sanitizer.setDocumentIaModelClassesOverride(List.of(TestIdentityTwoDDocModel.class));
            sanitizer.init();

            Document document = new Document();
            document.setDocumentCategory(DocumentCategory.IDENTIFICATION);
            document.setDocumentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD);

            BarcodeModel barcode = BarcodeModel.builder()
                    .type("2D_DOC")
                    .antsType("avis_imposition")
                    .rawData(java.util.Map.of("raw_key", "remove"))
                    .typedData(List.of(
                            GenericProperty.builder().name("doc_type").type(GenericProperty.TYPE_STRING).value("28").build(),
                            GenericProperty.builder().name("noise").type(GenericProperty.TYPE_STRING).value("remove").build()
                    ))
                    .build();

            ResultModel input = ResultModel.builder()
                    .extraction(null)
                    .barcodes(List.of(barcode))
                    .build();

            ResultModel output = sanitizer.sanitize(input, document);

            assertThat(output.getBarcodes()).hasSize(1);
            assertThat(output.getBarcodes().getFirst().getRawData()).isNull();
            assertThat(output.getBarcodes().getFirst().getTypedData())
                    .extracting(GenericProperty::getName)
                    .containsExactly("doc_type");
        }
    }
}

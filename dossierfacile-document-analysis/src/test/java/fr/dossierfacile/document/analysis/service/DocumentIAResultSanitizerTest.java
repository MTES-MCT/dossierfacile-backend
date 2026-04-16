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
import fr.dossierfacile.document.analysis.rule.validator.document_ia_model.TestIdentityNestedCollectionsModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia_model.TestIdentityTwoDDocModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia_model.TestOtherIdentityModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia_model.TestSubCategoryDiscriminatorModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    class testSanitizeSimpleModel {

        @Test
        void should_filter_root_properties_for_single_model() {
            DocumentIAResultSanitizer sanitizer = newSanitizer(List.of(TestIdentityModel.class));

            ResultModel output = sanitizer.sanitize(buildRootResultModel(true), identityDocument());

            assertThat(output.getExtraction().getProperties())
                    .extracting(GenericProperty::getName)
                    .containsExactlyInAnyOrder("nom_test", "prenom_test");
        }

        @Test
        void should_merge_allowed_root_properties_for_multiple_models() {
            DocumentIAResultSanitizer sanitizer = newSanitizer(List.of(TestIdentityModel.class, TestOtherIdentityModel.class));

            ResultModel output = sanitizer.sanitize(buildRootResultModel(true), identityDocument());

            assertThat(output.getExtraction().getProperties())
                    .extracting(GenericProperty::getName)
                    .containsExactlyInAnyOrder("nom_test", "prenom_test", "expiration");
        }
    }

    @Nested
    class testSanitizeNestedModels {

        @Test
        void should_clean_object_recursively() {
            DocumentIAResultSanitizer sanitizer = newSanitizer(List.of(TestIdentityNestedCollectionsModel.class));

            ResultModel output = sanitizer.sanitize(buildNestedResultModel(), identityDocument());

            GenericProperty holder = findByName(output, "holder");
            assertThat(holder.getType()).isEqualTo(GenericProperty.TYPE_OBJECT);
            assertThat(holder.getObjectValue())
                    .extracting(GenericProperty::getName)
                    .containsExactly("first_name");
        }

        @Test
        void should_clean_list_of_objects_recursively_and_keep_list_string() {
            DocumentIAResultSanitizer sanitizer = newSanitizer(List.of(TestIdentityNestedCollectionsModel.class));

            ResultModel output = sanitizer.sanitize(buildNestedResultModel(), identityDocument());

            assertThat(output.getExtraction().getProperties())
                    .extracting(GenericProperty::getName)
                    .containsExactlyInAnyOrder("tags", "holder", "beneficiaries");

            GenericProperty tags = findByName(output, "tags");
            assertThat(tags.getStringListValue()).containsExactly("tag-1", "tag-2");

            GenericProperty beneficiaries = findByName(output, "beneficiaries");
            assertThat(beneficiaries.getObjectListValue()).hasSize(2);
            assertThat(beneficiaries.getObjectListValue().get(0))
                    .extracting(GenericProperty::getName)
                    .containsExactlyInAnyOrder("first_name", "inner_tags");
            assertThat(beneficiaries.getObjectListValue().get(1))
                    .extracting(GenericProperty::getName)
                    .containsExactlyInAnyOrder("first_name", "inner_tags");

            // Verify LIST_STRING nested in each object is still present and untouched.
            assertThat(beneficiaries.getObjectListValue().get(0).stream()
                    .filter(p -> "inner_tags".equals(p.getName()))
                    .findFirst()
                    .orElseThrow()
                    .getStringListValue())
                    .containsExactly("a", "b");
            assertThat(beneficiaries.getObjectListValue().get(1).stream()
                    .filter(p -> "inner_tags".equals(p.getName()))
                    .findFirst()
                    .orElseThrow()
                    .getStringListValue())
                    .containsExactly("c", "d");
        }
    }

    @Nested
    class testSanitizeBarcodes {

        @Test
        void should_remove_fields_from_raw_data_and_keep_only_allowed_typed_data() {
            DocumentIAResultSanitizer sanitizer = newSanitizer(List.of(TestIdentityTwoDDocModel.class));

            ResultModel input = ResultModel.builder()
                    .barcodes(List.of(
                            BarcodeModel.builder()
                                    .rawData(buildSampleRawData("91239123ZSHFD", "JEAN DOE"))
                                    .typedData(List.of(
                                            GenericProperty.builder().name("doc_type").type(GenericProperty.TYPE_STRING).value("CNI").build(),
                                            GenericProperty.builder().name("reference_avis").type(GenericProperty.TYPE_STRING).value("ABCD").build(),
                                            GenericProperty.builder().name("noise").type(GenericProperty.TYPE_STRING).value("remove").build()
                                    ))
                                    .build()
                    ))
                    .build();

            ResultModel output = sanitizer.sanitize(input, identityDocument());

            assertThat(output.getBarcodes()).hasSize(1);
            assertThat(output.getBarcodes().get(0).getRawData()).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> sanitizedRawData = (Map<String, Object>) output.getBarcodes().get(0).getRawData();
            assertThat(sanitizedRawData)
                    .doesNotContainKey("fields")
                    .containsEntry("country", "FR")
                    .containsEntry("doc_type", "27")
                    .containsEntry("perimeter", "01")
            ;
            assertThat(output.getBarcodes().get(0).getTypedData())
                    .extracting(GenericProperty::getName)
                    .containsExactlyInAnyOrder("doc_type", "reference_avis");
        }

        @Test
        void should_remove_fields_from_raw_data_for_all_barcodes_even_when_typed_data_is_null_or_empty() {
            DocumentIAResultSanitizer sanitizer = newSanitizer(List.of(TestIdentityTwoDDocModel.class));

            ResultModel input = ResultModel.builder()
                    .barcodes(List.of(
                            BarcodeModel.builder().rawData(buildSampleRawData("1293129DSJFS", "JOHN DOE")).typedData(null).build(),
                            BarcodeModel.builder().rawData(buildSampleRawData("1293129DSJFS231", "DURAND DURANT")).typedData(List.of()).build()
                    ))
                    .build();

            ResultModel output = sanitizer.sanitize(input, identityDocument());

            assertThat(output.getBarcodes()).hasSize(2);
            assertThat(output.getBarcodes())
                    .allSatisfy(barcode -> {
                        assertThat(barcode.getRawData()).isInstanceOf(Map.class);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> rawData = (Map<String, Object>) barcode.getRawData();
                        assertThat(rawData).doesNotContainKey("fields");
                        assertThat(rawData).containsKeys("country", "doc_type", "perimeter");
                    });
            assertThat(output.getBarcodes().get(0).getTypedData()).isNull();
            assertThat(output.getBarcodes().get(1).getTypedData()).isEmpty();
        }

        private Map<String, Object> buildSampleRawData(String referenceAvis, String holder) {
            return Map.of(
                    "fields", Map.of(
                            "41", "1148283",
                            "43", "1",
                            "44", referenceAvis,
                            "45", "2025",
                            "46", holder,
                            "47", "238123812832183",
                            "4B", "12389213",
                            "4V", "0",
                            "4Y", "8 RUE DE LA PAIX/75000 PARIS"
                    ),
                    "country", "FR",
                    "doc_type", "27",
                    "perimeter", "01"
            );
        }
    }

    private DocumentIAResultSanitizer newSanitizer(List<Class<?>> modelClasses) {
        DocumentIAResultSanitizer sanitizer = new DocumentIAResultSanitizer();
        sanitizer.setDocumentIaModelClassesOverride(modelClasses);
        sanitizer.init();
        return sanitizer;
    }

    private Document identityDocument() {
        Document document = new Document();
        document.setDocumentCategory(DocumentCategory.IDENTIFICATION);
        document.setDocumentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD);
        return document;
    }

    private ResultModel buildRootResultModel(boolean includeExpiration) {
        List<GenericProperty> properties = new ArrayList<>(List.of(
                GenericProperty.builder().name("nom_test").type(GenericProperty.TYPE_STRING).value("Dupont").build(),
                GenericProperty.builder().name("prenom_test").type(GenericProperty.TYPE_STRING).value("Jean").build(),
                GenericProperty.builder().name("noise").type(GenericProperty.TYPE_STRING).value("remove").build()
        ));

        if (includeExpiration) {
            properties.add(GenericProperty.builder().name("expiration").type(GenericProperty.TYPE_STRING).value("ok").build());
        }

        return ResultModel.builder()
                .extraction(ExtractionModel.builder().properties(properties).build())
                .build();
    }

    private ResultModel buildNestedResultModel() {
        GenericProperty holder = GenericProperty.builder()
                .name("holder")
                .type(GenericProperty.TYPE_OBJECT)
                .value(List.of(
                        GenericProperty.builder().name("first_name").type(GenericProperty.TYPE_STRING).value("Jane").build(),
                        GenericProperty.builder().name("last_name").type(GenericProperty.TYPE_STRING).value("Doe").build()
                ))
                .build();

        GenericProperty beneficiaries = GenericProperty.builder()
                .name("beneficiaries")
                .type(GenericProperty.TYPE_LIST)
                .value(List.of(
                        GenericProperty.builder()
                                .name("item")
                                .type(GenericProperty.TYPE_OBJECT)
                                .value(List.of(
                                        GenericProperty.builder().name("first_name").type(GenericProperty.TYPE_STRING).value("Alice").build(),
                                        GenericProperty.builder().name("inner_tags").type(GenericProperty.TYPE_LIST).value(List.of("a", "b")).build(),
                                        GenericProperty.builder().name("last_name").type(GenericProperty.TYPE_STRING).value("Martin").build()
                                ))
                                .build(),
                        GenericProperty.builder()
                                .name("item")
                                .type(GenericProperty.TYPE_OBJECT)
                                .value(List.of(
                                        GenericProperty.builder().name("first_name").type(GenericProperty.TYPE_STRING).value("Bob").build(),
                                        GenericProperty.builder().name("inner_tags").type(GenericProperty.TYPE_LIST).value(List.of("c", "d")).build(),
                                        GenericProperty.builder().name("last_name").type(GenericProperty.TYPE_STRING).value("Durand").build()
                                ))
                                .build()
                ))
                .build();

        List<GenericProperty> properties = List.of(
                GenericProperty.builder().name("tags").type(GenericProperty.TYPE_LIST).value(List.of("tag-1", "tag-2")).build(),
                holder,
                beneficiaries,
                GenericProperty.builder().name("nested_noise").type(GenericProperty.TYPE_LIST).value(List.of("remove")).build()
        );

        return ResultModel.builder()
                .extraction(ExtractionModel.builder().properties(properties).build())
                .build();
    }

    private GenericProperty findByName(ResultModel resultModel, String name) {
        return resultModel.getExtraction().getProperties().stream()
                .filter(property -> name.equals(property.getName()))
                .findFirst()
                .orElseThrow();
    }
}

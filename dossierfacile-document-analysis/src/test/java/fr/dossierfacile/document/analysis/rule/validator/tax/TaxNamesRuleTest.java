package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.rule.TaxNamesRuleData;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TaxNamesRuleTest {

    private final TaxNamesRule rule = new TaxNamesRule();

    @ParameterizedTest(name = "{0}")
    @MethodSource("passedOrFailedCases")
    void should_return_expected_pass_or_fail_for_tax_name_matching(
            String ignoredCaseName,
            Tenant tenant,
            List<DocumentIAFileAnalysis> analyses,
            RuleValidatorOutput.RuleLevel expectedLevel
    ) {
        Document document = documentWithAnalysis(analyses, tenant);

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(expectedLevel);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_NAMES);
        assertThat(result.rule().getRuleData()).isInstanceOf(TaxNamesRuleData.class);
        TaxNamesRuleData data = (TaxNamesRuleData) result.rule().getRuleData();
        assertThat(data.extractedIdentities()).isNotEmpty();
    }

    @Test
    @DisplayName("Should be inconclusive when no tax document found")
    void should_be_inconclusive_when_no_tax_document() {
        Tenant tenant = Tenant.builder().lastName("DOE").firstName("JOHN").build();
        Document document = documentWithAnalysis(List.of(fakeOtherDocument()), tenant);

        RuleValidatorOutput result = rule.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    private static Stream<Arguments> passedOrFailedCases() {
        return Stream.of(
                Arguments.of(
                        "Should validate when first declarant names match",
                        tenant("DOE", "JOHN"),
                        List.of(fakeAvisImposition("DOE MIKEAL JOHN")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should validate with composed names",
                        tenant("SMITH", "Amélie Sarah Emeline"),
                        List.of(fakeAvisImposition("JOHN SMITH AMELIE")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should validate when second declarant matches",
                        tenant("DOE", "JANE"),
                        List.of(fakeAvisImposition("DOE JOHN", "DOE JANE")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should fail when names do not match",
                        tenant("SMITH", "JOHN"),
                        List.of(fakeAvisImposition("DOE JANE")),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should validate with multiple files if one matches",
                        tenant("DOE", "JOHN"),
                        List.of(fakeAvisImposition("SMITH JANE"), fakeAvisImposition("DOE JOHN")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should fail with multiple files if none match",
                        tenant("DOE", "JOHN"),
                        List.of(fakeAvisImposition("SMITH JANE"), fakeAvisImposition("COOPER ALICE")),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should validate with fuzzy first name matching",
                        tenant("DUPONT", "Alezandro"),
                        List.of(fakeAvisImposition("DUPONT Alessandro")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should not validate with fuzzy first name matching Levenshtein > 3",
                        tenant("DUPONT", "Alezandro"),
                        List.of(fakeAvisImposition("DUPONT Alesssandro")),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should validate with joint declaration and marital name",
                        tenant("DUPONT", "Alice"),
                        List.of(fakeAvisImposition("DOE ALICE", "DUPONT GAEL")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should not validate with composed names",
                        tenant("DE LARUE", "Alice"),
                        List.of(fakeAvisImposition("DE RENCOURT ALICE")),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should validated with prefered name",
                        tenant("Smith", "Agent", "DUPONT"),
                        List.of(fakeAvisImposition("Dupont Agent")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should validated with prefered name",
                        tenant("Smith", "Agent", "DUPONT"),
                        List.of(fakeAvisImposition("SMITH Agent")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should not validated",
                        tenant("Smith", "Agent"),
                        List.of(fakeAvisImposition("SMITH michel")),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should not validated",
                        tenant("Smith", "Agent"),
                        List.of(fakeAvisImposition("MICHEL Agent")),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should validated when identity inverted",
                        tenant("Smith", "Agent"),
                        List.of(fakeAvisImposition("Agent Smith")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should validated when accent char",
                        tenant("Smith", "aééééééé"),
                        List.of(fakeAvisImposition("SMITH aeeeeeee")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should failed when short last names",
                        tenant("XU", "de jean"),
                        List.of(fakeAvisImposition("de jean")),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should passed with exact short last names",
                        tenant("XU YI", "Jean"),
                        List.of(fakeAvisImposition("XU YI Jean")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should failed when short last names, otherwise rule is too permissive",
                        tenant("XU YI", "Jean"),
                        List.of(fakeAvisImposition("YI Jean")),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should failed short composed names, otherwise rule is too permissive",
                        tenant("SMITH DOE", "Jean"),
                        List.of(fakeAvisImposition("DOE Jean")),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should pass composed names from 2DDoc",
                        tenant("SMITH", "jean"),
                        List.of(fakeAvisImposition("Smith DO jean")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should pass composed names with - from tenant (hyphen as space in 2DDoc)",
                        tenant("SMITH-DOE", "jean"),
                        List.of(fakeAvisImposition("Smith Doe jean")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should passed composed names with - from tenant",
                        tenant("SMITH-DOE", "jean"),
                        List.of(fakeAvisImposition("SmithDoe jean")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should passed",
                        tenant("SMITH-DOE", "A"),
                        List.of(fakeAvisImposition("SMITH-DOE A")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should pass with hyphenated first names and second declarant mismatch",
                        tenant("Dubois", "Pierre-Paul"),
                        List.of(fakeAvisImposition("DUBOIS PIERRE PAUL", "DUBOIS MARIE CLAIRE")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should pass with multiple first names and hyphen",
                        tenant("DIAKITE", "Amadou-Oumar Diallo"),
                        List.of(fakeAvisImposition("DIAKITE AMADOU OUMAR")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should pass with composed tenant last name and partial barcode last name",
                        tenant("GONZALEZ RODRIGUEZ", "Carlos Manuel"),
                        List.of(fakeAvisImposition("GONZALEZ CARLOS")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should pass with accent normalization in first name",
                        tenant("Fontaine", "Marie-Cécile"),
                        List.of(fakeAvisImposition("FONTAINE LUCAS", "FONTAINE MARIE CECILE")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should pass with hyphenated tenant identity normalized as spaces",
                        tenant("Dubois-Rivière", "Thomas-Antoine"),
                        List.of(fakeAvisImposition("DUBOIS RIVIERE THOMAS ANTOINE", "LEFEVRE CAMILLE")),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should pass with apostrophe and multiple composed last names",
                        tenant("OMAR D'SOU KONATE", "Aïcha"),
                        List.of(fakeAvisImposition("DSOU AICHA")),
                        RuleValidatorOutput.RuleLevel.PASSED
                )
        );
    }

    // ==========================================
    // Fixtures
    // ==========================================

    private static Tenant tenant(String lastName, String firstName) {
        return Tenant.builder().lastName(lastName).firstName(firstName).build();
    }

    private static Tenant tenant(String lastName, String firstName, String preferredName) {
        return Tenant.builder().lastName(lastName).firstName(firstName).preferredName(preferredName).build();
    }

    private static Document documentWithAnalysis(List<DocumentIAFileAnalysis> analysiss, Tenant tenant) {
        List<File> files = analysiss.stream()
                .map(analysis -> File.builder()
                        .documentIAFileAnalysis(analysis)
                        .build())
                .toList();
        return Document.builder()
                .tenant(tenant)
                .files(files)
                .build();
    }

    private static DocumentIAFileAnalysis fakeAvisImposition(String declarant1Name) {
        return fakeAvisImposition(declarant1Name, null);
    }

    private static DocumentIAFileAnalysis fakeAvisImposition(String declarant1Name, String declarant2Name) {
        List<GenericProperty> typedData = List.of(
                GenericProperty.builder().name("doc_type").value("28").type("string").build(),
                GenericProperty.builder().name("declarant_1").value(declarant1Name).type("string").build(),
                GenericProperty.builder().name("declarant_2").value(declarant2Name).type("string").build()
        );

        BarcodeModel barcode = BarcodeModel.builder()
                .pageNumber(1)
                .type("2D_DOC")
                .isValid(true)
                .rawData(null)
                .typedData(typedData)
                .antsType("avis_imposition")
                .build();

        ResultModel result = ResultModel.builder()
                .barcodes(List.of(barcode))
                .build();

        return DocumentIAFileAnalysis.builder()
                .documentIaExecutionId("exec-id")
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }

    private static DocumentIAFileAnalysis fakeOtherDocument() {
        BarcodeModel barcode = BarcodeModel.builder()
                .type("2D_DOC")
                .isValid(true)
                .antsType("other_type")
                .build();
        ResultModel result = ResultModel.builder()
                .barcodes(List.of(barcode))
                .build();
        return DocumentIAFileAnalysis.builder()
                .documentIaExecutionId("exec-id")
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }
}

package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TaxLeafRuleBTest {

    private final TaxLeafRuleB rule = new TaxLeafRuleB();

    @ParameterizedTest(name = "{0}")
    @MethodSource("taxLeafCases")
    void should_return_expected_level_based_on_total_pages(
            String ignoredCaseName,
            List<Integer> pages,
            RuleValidatorOutput.RuleLevel expectedLevel
    ) {
        Document document = documentWithPages(pages);

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(expectedLevel);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_LEAF);
    }

    private static Stream<Arguments> taxLeafCases() {
        return Stream.of(
                Arguments.of(
                        "Should fail when no file",
                        List.of(),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should fail when total pages is one",
                        List.of(1),
                        RuleValidatorOutput.RuleLevel.FAILED
                ),
                Arguments.of(
                        "Should pass when one file has two pages",
                        List.of(2),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should pass when two files sum to two pages",
                        List.of(1, 1),
                        RuleValidatorOutput.RuleLevel.PASSED
                ),
                Arguments.of(
                        "Should fail when multiple files sum to less than two pages",
                        List.of(1, 0, 0),
                        RuleValidatorOutput.RuleLevel.FAILED
                )
        );
    }

    private static Document documentWithPages(List<Integer> pages) {
        List<File> files = pages.stream()
                .map(numberOfPages -> File.builder().numberOfPages(numberOfPages).build())
                .toList();

        return Document.builder()
                .files(files)
                .build();
    }
}


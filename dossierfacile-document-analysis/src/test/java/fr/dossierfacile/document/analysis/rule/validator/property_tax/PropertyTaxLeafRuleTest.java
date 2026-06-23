package fr.dossierfacile.document.analysis.rule.validator.property_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyTaxLeafRuleTest {

    private final PropertyTaxLeafRule rule = new PropertyTaxLeafRule();

    @Test
    @DisplayName("Should pass with 2 pages (limit)")
    void should_pass_with_two_pages() {
        RuleValidatorOutput result = rule.validate(documentWithPages(2));

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_PROPERTY_TAX_LEAF);
    }

    @Test
    @DisplayName("Should pass when pages are spread across multiple files")
    void should_pass_with_pages_across_files() {
        RuleValidatorOutput result = rule.validate(documentWithPages(1, 1));

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("Should fail with a single page")
    void should_fail_with_single_page() {
        RuleValidatorOutput result = rule.validate(documentWithPages(1));

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(result.isBlocking()).isTrue();
    }

    @Test
    @DisplayName("Should fail with no page")
    void should_fail_with_no_page() {
        RuleValidatorOutput result = rule.validate(documentWithPages(0));

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    private static Document documentWithPages(int... pages) {
        List<File> files = Arrays.stream(pages)
                .mapToObj(nbPages -> File.builder().numberOfPages(nbPages).build())
                .toList();
        return Document.builder()
                .tenant(Tenant.builder().build())
                .files(files)
                .build();
    }
}

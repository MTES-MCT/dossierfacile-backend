package fr.dossierfacile.document.analysis.rule.validator.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityMatchUtilTest {

    @Test
    @DisplayName("Should merge both sources and drop entries contained in a more complete one")
    void should_merge_and_deduplicate_identities() {
        List<String> proprietaires = List.of("LECONTE CHRISTOPHE MARCEL PAUL", "GIFFARD VALERIE HELENE PIERRETTE");
        List<String> destinataires = List.of("LECONTE CHRISTOPHE", "LECONTE VALERIE");

        List<String> result = IdentityMatchUtil.mergeAndDeduplicateIdentities(proprietaires, destinataires);

        assertThat(result).containsExactlyInAnyOrder(
                "LECONTE CHRISTOPHE MARCEL PAUL",
                "LECONTE VALERIE",
                "GIFFARD VALERIE HELENE PIERRETTE"
        );
        assertThat(result).doesNotContain("LECONTE CHRISTOPHE");
    }

    @Test
    @DisplayName("Should ignore null sources and null/blank entries (and trim)")
    void should_ignore_null_and_blank() {
        List<String> result = IdentityMatchUtil.mergeAndDeduplicateIdentities(
                null,
                Arrays.asList("  DUPONT JEAN  ", null, "", "   ")
        );

        assertThat(result).containsExactly("DUPONT JEAN");
    }

    @Test
    @DisplayName("Should keep a single entry for normalized duplicates (case/accents)")
    void should_deduplicate_normalized_duplicates() {
        List<String> result = IdentityMatchUtil.mergeAndDeduplicateIdentities(
                List.of("Dupont Jean"),
                List.of("DUPONT JEAN")
        );

        assertThat(result).hasSize(1);
    }
}

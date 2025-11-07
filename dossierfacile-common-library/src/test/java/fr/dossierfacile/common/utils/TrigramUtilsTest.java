package fr.dossierfacile.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TrigramUtilsTest {

    @Test
    @DisplayName("compute returns empty when last name is blank")
    void compute_blank() {
        assertThat(TrigramUtils.compute(" ")).isEmpty();
        assertThat(TrigramUtils.compute(null)).isEmpty();
    }

    @Test
    @DisplayName("compute returns three letters when name longer than three letters")
    void compute_threeLetters() {
        Optional<String> result = TrigramUtils.compute("Martin");
        assertThat(result).contains("MAR");
    }

    @Test
    @DisplayName("compute handles spaces inside name")
    void compute_withAccent() {
        Optional<String> result = TrigramUtils.compute("Rémi");
        assertThat(result).contains("REM");
    }

    @Test
    @DisplayName("compute removes diacritics and punctuation")
    void compute_normalizes() {
        Optional<String> result = TrigramUtils.compute("d'Anjou");
        assertThat(result).contains("DAN");
    }

    @Test
    @DisplayName("compute keeps available letters when shorter than three")
    void compute_shortName() {
        Optional<String> result = TrigramUtils.compute("Xu");
        assertThat(result).contains("XU");
    }

    @Test
    @DisplayName("compute handles spaces inside name")
    void compute_withSpaces() {
        Optional<String> result = TrigramUtils.compute("de Ville");
        assertThat(result).contains("DEV");
    }

    @Test
    @DisplayName("compute handles spaces inside name")
    void compute_withSpecialCharacters() {
        Optional<String> result = TrigramUtils.compute("N’Bala");
        assertThat(result).contains("NBA");
    }
}


package fr.dossierfacile.api.dossierfacileapiowner.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidDpeNumberValidatorTest {

    private final ValidDpeNumberValidator validator = new ValidDpeNumberValidator();

    @Test
    void isValid_shouldAcceptNullOrBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void isValid_shouldAcceptValid13CharacterAlphanumericDpeNumber() {
        assertThat(validator.isValid("2337E0363555K", null)).isTrue();
        assertThat(validator.isValid("2134E1234567A", null)).isTrue();
        assertThat(validator.isValid("1234567890123", null)).isTrue();
        assertThat(validator.isValid("ABCDEFGHIJKLM", null)).isTrue();
    }

    @Test
    void isValid_shouldRejectInvalidLengths() {
        // Less than 13 characters
        assertThat(validator.isValid("2337E0363555", null)).isFalse();
        // More than 13 characters
        assertThat(validator.isValid("2337E0363555K1", null)).isFalse();
    }

    @Test
    void isValid_shouldRejectSpecialCharacters() {
        assertThat(validator.isValid("2337E0363555!", null)).isFalse();
        assertThat(validator.isValid("2337E-0363555", null)).isFalse();
        assertThat(validator.isValid("2337E 0363555", null)).isFalse();
    }
}

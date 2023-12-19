package fr.dossierfacile.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class DateBasedFeatureToggleTest {

    @Test
    void should_be_active_if_no_date() {
        DateBasedFeatureToggle toggle = new DateBasedFeatureToggle("");

        assertThat(toggle.isActive()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void should_be_active(int minusDays) {
        String activationDate = LocalDate.now().minusDays(minusDays).format(DateTimeFormatter.ISO_DATE);
        DateBasedFeatureToggle toggle = new DateBasedFeatureToggle(activationDate);

        assertThat(toggle.isActive()).isTrue();
    }

    @Test
    void should_not_be_active() {
        String activationDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);
        DateBasedFeatureToggle toggle = new DateBasedFeatureToggle(activationDate);

        assertThat(toggle.isActive()).isFalse();
    }

    @Test
    void should_print_activation_date() {
        DateBasedFeatureToggle toggle = new DateBasedFeatureToggle("2023-12-18");

        assertThat(toggle.getActivationDate()).isEqualTo("2023-12-18");
    }

}
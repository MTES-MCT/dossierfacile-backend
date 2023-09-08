package fr.dossierfacile.api.front.validator;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class OtherResidencyToggleTest {

    @Test
    void should_be_active_if_no_date() {
        OtherResidencyToggle toggle = new OtherResidencyToggle("");

        assertThat(toggle.isNotActive()).isFalse();
    }

    @Test
    void should_be_active() {
        String activationDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        OtherResidencyToggle toggle = new OtherResidencyToggle(activationDate);

        assertThat(toggle.isNotActive()).isFalse();
    }

    @Test
    void should_not_be_active() {
        String activationDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);
        OtherResidencyToggle toggle = new OtherResidencyToggle(activationDate);

        assertThat(toggle.isNotActive()).isTrue();
    }

}
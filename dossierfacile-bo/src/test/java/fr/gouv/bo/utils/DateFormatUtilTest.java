package fr.gouv.bo.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateFormatUtilTest {

    @Test
    void should_display_date_relative_to_now() {
        LocalDateTime date = LocalDateTime.now().minusDays(1).minusHours(2).minusMinutes(3);
        String formatted = DateFormatUtil.relativeToNow(date);
        assertThat(formatted).isEqualTo("il y a 1 jour 2 heures 3 minutes");
    }

}
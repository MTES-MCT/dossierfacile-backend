package fr.gouv.bo.utils;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateFormatUtilTest {

    @Test
    void should_display_date_precisely_relative_to_now() {
        LocalDateTime date = LocalDateTime.now().minusDays(1).minusHours(2).minusMinutes(3);
        String formatted = DateFormatUtil.formatPreciselyRelativeToNow(date);
        assertThat(formatted).isEqualTo("il y a 1 jour 2 heures 3 minutes");
    }

    @Test
    void should_display_date_relative_to_now() {
        LocalDateTime date = LocalDateTime.now().minusDays(1).minusHours(2).minusMinutes(3);
        String formatted = DateFormatUtil.formatRelativeToNow(date);
        assertThat(formatted).isEqualTo("il y a 1 jour");
    }

    @Test
    void should_format_month_placeholders() {
        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            LocalDate testDate = LocalDate.of(2025, 4, 28);
            mockedStatic.when(LocalDate::now).thenReturn(testDate);
            String message = "Les mois de {mois}, {moisN-1}, {moisN-2} et {moisN-3}";
            String formatted = DateFormatUtil.replaceMonthPlaceholder(message);
            assertThat(formatted).isEqualTo("Les mois de Avril, Mars, Février et Janvier");
        }
    }

    @Test
    void should_format_month_placeholders_at_start_of_month() {
        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            LocalDate testDate = LocalDate.of(2025, 4, 10);
            mockedStatic.when(LocalDate::now).thenReturn(testDate);
            String message = "Les mois de {mois}, {moisN-1}, {moisN-2} et {moisN-3}";
            String formatted = DateFormatUtil.replaceMonthPlaceholder(message);
            assertThat(formatted).isEqualTo("Les mois de Mars, Février, Janvier et Décembre");
        }
    }

    @Test
    void should_ignore_invalid_placeholders() {
        String message = "Les mois de {moisn-1}, {autre}";
        String formatted = DateFormatUtil.replaceMonthPlaceholder(message);
        assertThat(formatted).isEqualTo(message);
    }

}
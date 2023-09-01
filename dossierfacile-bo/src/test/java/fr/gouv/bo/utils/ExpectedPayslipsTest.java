package fr.gouv.bo.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ExpectedPayslipsTest {

    @Test
    void should_return_expected_payslip_months_for_date_before_15() {
        LocalDate today = LocalDate.of(2023, 9, 1);
        ExpectedPayslips expectedPayslips = ExpectedPayslips.atDate(today);

        assertThat(expectedPayslips).isEqualTo(new ExpectedPayslips(
                List.of(Month.MAY, Month.JUNE, Month.JULY),
                Month.AUGUST
        ));
    }

    @Test
    void should_return_expected_payslip_months_for_date_after_15() {
        LocalDate today = LocalDate.of(2023, 9, 16);
        ExpectedPayslips expectedPayslips = ExpectedPayslips.atDate(today);

        assertThat(expectedPayslips).isEqualTo(new ExpectedPayslips(
                List.of(Month.JUNE, Month.JULY, Month.AUGUST),
                null
        ));
    }

    @Test
    void should_format_list() {
        LocalDate today = LocalDate.of(2023, 9, 1);
        ExpectedPayslips expectedPayslips = ExpectedPayslips.atDate(today);

        assertThat(expectedPayslips.format(Locale.FRANCE))
                .isEqualTo("mai, juin, juillet (août si possible)");
    }

}
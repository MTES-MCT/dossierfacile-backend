package fr.gouv.bo.utils;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public record ExpectedPayslips(List<Month> expectedMonths, Month optionalMonth) {

    public static ExpectedPayslips atDate(LocalDate date) {
        Month currentMonth = date.getMonth();
        if (date.getDayOfMonth() < 15) {
            return new ExpectedPayslips(getThreeMonthsPrecedingAndIncluding(currentMonth.minus(2)), currentMonth.minus(1));
        } else {
            return new ExpectedPayslips(getThreeMonthsPrecedingAndIncluding(currentMonth.minus(1)), null);
        }
    }

    private static List<Month> getThreeMonthsPrecedingAndIncluding(Month month) {
        return List.of(month.minus(2), month.minus(1), month);
    }

    public String format(Locale locale) {
        String result = expectedMonths.stream()
                .map(month -> month.getDisplayName(TextStyle.FULL, locale))
                .collect(Collectors.joining(", "));
        if (optionalMonth != null) {
            result += " (%s si possible)".formatted(optionalMonth.getDisplayName(TextStyle.FULL, locale));
        }
        return result;
    }

}

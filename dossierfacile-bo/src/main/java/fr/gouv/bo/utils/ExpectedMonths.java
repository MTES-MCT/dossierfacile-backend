package fr.gouv.bo.utils;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public record ExpectedMonths(List<Month> requiredMonths, Month optionalMonth) {

    public static ExpectedMonths forPayslips(LocalDate date) {
        Month currentMonth = date.getMonth();
        if (date.getDayOfMonth() < 15) {
            return threeMonthsPrecedingAndIncluding(currentMonth.minus(2), true);
        } else {
            return threeMonthsPrecedingAndIncluding(currentMonth.minus(1), false);
        }
    }

    public static ExpectedMonths forRentReceipt(LocalDate date) {
        Month currentMonth = date.getMonth();
        if (date.getDayOfMonth() < 15) {
            return threeMonthsPrecedingAndIncluding(currentMonth.minus(3), true);
        } else {
            return threeMonthsPrecedingAndIncluding(currentMonth.minus(2), true);
        }
    }

    private static ExpectedMonths threeMonthsPrecedingAndIncluding(Month month, boolean addOptionalNextMonth) {
        List<Month> requiredMonths = List.of(month.minus(2), month.minus(1), month);
        return new ExpectedMonths(requiredMonths, addOptionalNextMonth ? month.plus(1) : null);
    }

    public String format(Locale locale) {
        String result = requiredMonths.stream()
                .map(month -> month.getDisplayName(TextStyle.FULL, locale))
                .collect(Collectors.joining(", "));
        if (optionalMonth != null) {
            result += " (%s si possible)".formatted(optionalMonth.getDisplayName(TextStyle.FULL, locale));
        }
        return result;
    }

}

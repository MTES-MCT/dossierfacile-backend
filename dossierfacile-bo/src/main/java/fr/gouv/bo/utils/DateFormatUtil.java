package fr.gouv.bo.utils;

import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class DateFormatUtil {

    private static final Locale LOCALE = Locale.FRANCE;
    private static final PrettyTime prettyTime = new PrettyTime(LOCALE);

    public static String formatPreciselyRelativeToNow(LocalDateTime localDateTime) {
        return prettyTime.format(getDurations(localDateTime));
    }

    public static String formatRelativeToNow(LocalDateTime localDateTime) {
        List<Duration> durations = getDurations(localDateTime);
        return prettyTime.format(durations.get(0));
    }

    private static List<Duration> getDurations(LocalDateTime localDateTime) {
        return prettyTime.calculatePreciseDuration(localDateTime);
    }

    public static String getExpectedPayslipMonths() {
        return ExpectedMonths.forPayslips(LocalDate.now()).format(LOCALE);
    }

    public static String getExpectedRentReceiptMonths() {
        return ExpectedMonths.forRentReceipt(LocalDate.now()).format(LOCALE);
    }

    private static String formatMonth(Month month) {
        String name = month.getDisplayName(TextStyle.FULL, LOCALE);
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static String replaceMonthPlaceholder(String message) {
        LocalDate now = LocalDate.now();
        Month month = now.getDayOfMonth() < 16 ? now.getMonth().minus(1) : now.getMonth();
        String output = message.replace("{mois}", formatMonth(month));
        for (int i = 1; i <= 6; i++) {
            output = output.replace(String.format("{moisN-%d}", i), formatMonth(month.minus(i)));
        }
        return output;
    }


}

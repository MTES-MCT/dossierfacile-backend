package fr.gouv.bo.utils;

import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

}

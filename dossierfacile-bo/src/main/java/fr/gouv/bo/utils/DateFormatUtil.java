package fr.gouv.bo.utils;

import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class DateFormatUtil {

    public static String relativeToNow(LocalDateTime localDateTime) {
        PrettyTime prettyTime = new PrettyTime(Locale.FRANCE);
        List<Duration> durations = prettyTime.calculatePreciseDuration(localDateTime);
        return prettyTime.format(durations);
    }

}

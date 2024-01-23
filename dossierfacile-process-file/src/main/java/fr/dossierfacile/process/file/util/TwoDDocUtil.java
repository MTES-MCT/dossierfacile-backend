package fr.dossierfacile.process.file.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TwoDDocUtil {
    public static LocalDate getLocalDateFrom2DDocHexDate(String hexDateFrom200) {
        long dailyCount = Long.parseLong(hexDateFrom200, 16);
        return LocalDate.of(2000, 1, 1).plusDays(dailyCount);
    }

    public static String get2DDocHexDateFromLocalDate(LocalDate localDate) {
        return String.format("%04X", LocalDate.of(2000, 1, 1).until(localDate, ChronoUnit.DAYS));
    }
}
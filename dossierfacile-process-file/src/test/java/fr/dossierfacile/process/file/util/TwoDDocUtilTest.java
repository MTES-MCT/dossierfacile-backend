package fr.dossierfacile.process.file.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class TwoDDocUtilTest {
    @Test
    void check_date() {
        Assertions.assertEquals(LocalDate.of(2023, 12, 1), TwoDDocUtil.getLocalDateFrom2DDocHexDate("221F"));
    }

    @Test
    void check_hex() {
        Assertions.assertEquals("221F", TwoDDocUtil.get2DDocHexDateFromLocalDate(LocalDate.of(2023, 12, 1)));
    }
}
package fr.dossierfacile.process.file.service.parsers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

class TaxIncomeLeafParserTest {
    private final TaxIncomeLeafParser taxIncomeLeafParser = new TaxIncomeLeafParser();


    @Test
    void test_regexp_pageInfo() {
        String test = "Test Feuillet n° : 1 / 3";
        Matcher matcher = taxIncomeLeafParser.pageInfoPattern.matcher(test);
        matcher.find();
        Assertions.assertEquals(1, Integer.parseInt(matcher.group(1)));
        Assertions.assertEquals(3, Integer.parseInt(matcher.group(2)));
    }

    @Test
    void test_regexp_incomeYear() {
        String test = "revenus de  2022";

        Matcher matcher = taxIncomeLeafParser.incomeYearPattern.matcher(test);
        matcher.find();
        Assertions.assertEquals(2022, Integer.parseInt(matcher.group(1)));
    }

    @Test
    void test_regexp_fiscalNumberPattern() {
        String test = "Test n° fiscal : 230418045 743 7034";

        Matcher matcher = taxIncomeLeafParser.fiscalNumberPattern.matcher(test);
        matcher.find();
        Assertions.assertEquals("230418045 743 7034", matcher.group(1));
    }
}
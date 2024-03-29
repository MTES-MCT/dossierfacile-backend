package fr.dossierfacile.process.file.service.parsers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
class TaxAssessmentParserTest {
    private final TaxAssessmentParser taxAssessmentParser = new TaxAssessmentParser(new TaxIncomeLeafParser());
    private final TaxAssessment2Parser taxAssessment2Parser = new TaxAssessment2Parser(new TaxIncomeLeafParser());

    @Test
    void parse() {
        // Set lib path: System.setProperty("jna.library.path", "/usr/local/lib");
        // Set to test environment: "TESSDATA_PREFIX","/usr/..../share/tessdata"
        File file = new File(this.getClass().getResource("/documents/tmpfake/taxincome.pdf").getFile());

        var doc = taxAssessmentParser.parse(file);
        System.out.print(doc);
    }
    @Test
    void parse2() {
        // Set lib path: System.setProperty("jna.library.path", "/opt/homebrew/lib");
        // Set to test environment: "TESSDATA_PREFIX","/usr/..../share/tessdata"
        System.setProperty("jna.library.path", "/opt/homebrew/lib");
        File file = new File(this.getClass().getResource("/documents/tmpfake/tax_assessment_2022.pdf").getFile());

        var doc = taxAssessment2Parser.parse(file);
        System.out.print(doc);
    }

}
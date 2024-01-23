package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.PublicPayslipFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
public class PublicPayslipFileParserTest {
    private final PublicPayslipParser parser = new PublicPayslipParser();

    @Test
    void parse() {
        File file = new File(this.getClass().getResource("/documents/fake_payslip.pdf").getFile());

        PublicPayslipFile doc = parser.parse(file);

        System.out.println(doc.getMonth());
        System.out.println(doc.getNetTaxableIncome());
        System.out.println(doc.getCumulativeNetTaxableIncome());
        System.out.println(doc.getFullname());
    }
}
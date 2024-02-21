package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.PayslipFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
class PublicPayslipFileParserTest {
    private final PublicPayslipParser parser = new PublicPayslipParser();

    @Test
    void parse() {
        File file = new File(this.getClass().getResource("/documents/fake_payslip.pdf").getFile());

        PayslipFile doc = parser.parse(file);

        System.out.println(doc);
    }
}
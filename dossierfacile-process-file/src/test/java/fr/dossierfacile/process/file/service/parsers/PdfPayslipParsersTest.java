package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.PayslipFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
class PdfPayslipParsersTest {
    private final PdfPayslipStd1Parser parser = new PdfPayslipStd1Parser();
    private final PdfPayslipStd3Parser parser3 = new PdfPayslipStd3Parser();

    @Test
    void parse() {
        File file = new File(this.getClass().getResource("/documents/tmpfake/fakepayslip_2.pdf").getFile());

        PayslipFile doc = parser.parse(file);
        System.out.print(doc);
    }
    @Test
    void parse3() {
        File file = new File(this.getClass().getResource("/documents/tmpfake/payslip_std3.pdf").getFile());

        PayslipFile doc = parser3.parse(file);
        System.out.print(doc);
    }

}
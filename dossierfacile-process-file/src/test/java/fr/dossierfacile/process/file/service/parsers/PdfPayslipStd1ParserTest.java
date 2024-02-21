package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.PayslipFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
class PdfPayslipStd1ParserTest {


    private final PdfPayslipStd1Parser parser = new PdfPayslipStd1Parser();

    @Test
    void parse() {
        File file = new File(this.getClass().getResource("/documents/fake_payslipStd1.pdf").getFile());

        PayslipFile doc = parser.parse(file);
        System.out.print(doc);
    }
}
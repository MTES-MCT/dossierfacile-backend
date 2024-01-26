package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
class TaxIncomeParserTest {
    private final TaxIncomeParser taxIncomeParser = new TaxIncomeParser(new TaxIncomeLeafParser());

    @Test
    void parse() {
        // Set lib path: System.setProperty("jna.library.path", "/usr/local/lib");
        // Set to test environment: "TESSDATA_PREFIX","/usr/..../share/tessdata"
        File file = new File(this.getClass().getResource("/documents/taxincome.pdf").getFile());

        TaxIncomeMainFile doc = taxIncomeParser.parse(file);
        System.out.print(doc);
    }
}
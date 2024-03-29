package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
class RentalReceipt3FParserTest {
    private final RentalReceipt3FParser parser = new RentalReceipt3FParser();

    @Test
    void parse() {
        //TESSDATA_PREFIX=/usr/local/share/tessdata
        //System.setProperty("jna.library.path", "/usr/local/lib");
        File file = new File(this.getClass().getResource("/documents/tmpfake/quittance3F.pdf").getFile());

        RentalReceiptFile doc = parser.parse(file);
        System.out.print(doc);
    }
}
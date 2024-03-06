package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
class RentalReceiptStdOneParserTest {
    private final RentalReceiptParser parser = new RentalReceiptParser();

    @Test
    void parse() {
        File file = new File(this.getClass().getResource("/fakeStd1.pdf").getFile());

        RentalReceiptFile doc = parser.parse(file);
        System.out.print(doc);
    }
}
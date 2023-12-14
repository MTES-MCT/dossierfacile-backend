package fr.dossierfacile.process.file.service.ocr;

import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuaranteeVisaleParserTest {
    private final GuaranteeVisaleParser visaleParser = new GuaranteeVisaleParser();

    @Disabled
    @Test
    void parse() {
        // Set lib path: System.setProperty("jna.library.path", "/usr/local/lib");
        // Set to test environment: "TESSDATA_PREFIX","/usr/..../share/tessdata"
        File file = new File(this.getClass().getResource("/fakevisale.pdf").getFile());

        GuaranteeProviderFile doc = visaleParser.parse(file);
        System.out.print(doc);

    }
}
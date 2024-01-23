package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

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
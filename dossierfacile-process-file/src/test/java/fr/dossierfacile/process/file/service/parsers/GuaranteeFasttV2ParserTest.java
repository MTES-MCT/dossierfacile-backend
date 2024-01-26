package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
class GuaranteeFasttV2ParserTest {
    private final GuaranteeFasttV2Parser parser = new GuaranteeFasttV2Parser();

    @Test
    void parse() {
        File file = new File(this.getClass().getResource("/fasttN2ZT.pdf").getFile());

        GuaranteeProviderFile doc = parser.parse(file);
        System.out.print(doc);
    }
    @Test
    void parse_failed() {
        File file = new File(this.getClass().getResource("/fasttN1ZT.pdf").getFile());

        GuaranteeProviderFile doc = parser.parse(file);
        System.out.print(doc);
    }
}
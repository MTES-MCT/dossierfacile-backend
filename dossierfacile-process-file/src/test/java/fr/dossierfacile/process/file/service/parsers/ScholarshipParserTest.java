package fr.dossierfacile.process.file.service.parsers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
class ScholarshipParserTest {
    private final ScholarshipCROUSParser scholarshipCROUSParser = new ScholarshipCROUSParser();

    @Test
    void parse() {
        // Set lib path: System.setProperty("jna.library.path", "/usr/local/lib");
        // Set to test environment: "TESSDATA_PREFIX","/usr/..../share/tessdata"
        File file = new File(this.getClass().getResource("/documents/tmpfake/crous1.pdf").getFile());

        var doc = scholarshipCROUSParser.parse(file);
        System.out.print(doc);
    }

}
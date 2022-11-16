package fr.dossierfacile.process.file.service;

import fr.dossierfacile.process.file.service.interfaces.ApiTesseract;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@Disabled
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class ApiTesseractTest {

    @Autowired
    private ApiTesseract apiTesseract;

    @Test
    void callTesseractApi() {
        // Set lib path: System.setProperty("jna.library.path", "/usr/..../lib");
        // Set to test environment: "TESSDATA_PREFIX","/usr/..../share/tessdata"
        String tesseractResult = apiTesseract.extractText(new File(this.getClass().getResource("/testocr.png").getFile()));
        System.err.println("tesseractResult2=" + tesseractResult);
        String expected = "This is a lot of 12 point text to test the\n" +
                "ocr code and see if it works on all types\n" +
                "of file format.\n" +
                "\n" +
                "The quick brown dog jumped over the\n" +
                "lazy fox. The quick brown dog jumped\n" +
                "over the lazy fox. The quick brown dog\n" +
                "jumped over the lazy fox. The quick\n" +
                "brown dog jumped over the lazy fox.\n";
        Assertions.assertEquals(expected, tesseractResult);
    }

}

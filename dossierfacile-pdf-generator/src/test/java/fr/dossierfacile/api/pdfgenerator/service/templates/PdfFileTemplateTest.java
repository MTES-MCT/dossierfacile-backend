package fr.dossierfacile.api.pdfgenerator.service.templates;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PdfFileTemplateTest {

    @EnumSource
    @ParameterizedTest
    void name(PdfFileTemplate template) throws IOException {
        InputStream inputStream = template.getInputStream();
        assertDoesNotThrow(() -> PDDocument.load(inputStream));
    }

}
package fr.dossierfacile.api.pdfgenerator.service.templates;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PdfFileTemplateTest {

    @EnumSource
    @ParameterizedTest
    void should_load_pdf(PdfFileTemplate template) {
        assertDoesNotThrow(template::load);
    }

}
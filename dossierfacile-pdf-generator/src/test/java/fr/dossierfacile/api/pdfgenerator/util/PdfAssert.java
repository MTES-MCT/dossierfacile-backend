package fr.dossierfacile.api.pdfgenerator.util;

import de.redsix.pdfcompare.CompareResultImpl;
import de.redsix.pdfcompare.PdfComparator;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PdfAssert extends AbstractAssert<PdfAssert, Path> {

    public static PdfAssert assertThat(Path actual) {
        return new PdfAssert(actual);
    }

    public static PdfAssert assertThat(String actualPath) {
        return new PdfAssert(actualPath);
    }

    protected PdfAssert(Path actual) {
        super(actual, PdfAssert.class);
    }

    protected PdfAssert(String actualPath) {
        super(Path.of(actualPath), PdfAssert.class);
    }

    @SneakyThrows
    public PdfAssert isEqualTo(Path expected) {
        CompareResultImpl compareResult = new PdfComparator<>(expected, actual).compare();
        if (compareResult.isNotEqual()) {
            failWithMessage("Differences found in PDFs on page(s) %s", compareResult.getPagesWithDifferences());
        }
        return this;
    }

    public PdfAssert isEqualTo(String expectedPath) {
        return isEqualTo(Paths.get(expectedPath));
    }

    public PdfAssert containsText(String expectedText) {
        String actualText = extractTextFromPdf(actual);
        Assertions.assertThat(actualText).isEqualTo(expectedText);
        return this;
    }

    @SneakyThrows
    private static String extractTextFromPdf(Path path) {
        PDDocument pdf = Loader.loadPDF(path.toFile());
        return new PDFTextStripper().getText(pdf);
    }

}

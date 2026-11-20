package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.util.Fonts;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Builds PDFs made of text, like the sworn statements that DossierFacile generates. */
final class TestPdfs {

    private TestPdfs() {}

    /** Returns a PDF with one page per text. */
    static byte[] statement(String... texts) {
        try (PDDocument document = new PDDocument()) {
            PDFont font = Fonts.MARIANNE_REGULAR.load(document);
            for (String text : texts) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 14);
                    content.newLineAtOffset(60, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            return save(document);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static byte[] rotated(byte[] pdf, int rotation) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            document.getPages().forEach(page -> page.setRotation(rotation));
            return save(document);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] save(PDDocument document) throws IOException {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        document.save(pdf);
        return pdf.toByteArray();
    }
}

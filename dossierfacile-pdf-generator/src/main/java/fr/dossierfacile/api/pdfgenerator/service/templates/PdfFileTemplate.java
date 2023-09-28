package fr.dossierfacile.api.pdfgenerator.service.templates;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.InputStream;

public enum PdfFileTemplate {

    FIRST_TABLE_OF_CONTENT_PAGE("template_Dossier_PDF_first_page_1.pdf"),
    OTHER_TABLE_OF_CONTENT_PAGES("template_Dossier_PDF_first_page_2.pdf"),
    ATTACHMENTS_AND_CLARIFICATIONS("template_Dossier_PDF_attachments_and_clarification.pdf"),
    DOCUMENT_FINANCIAL("template_document_financial.pdf"),
    DOCUMENT_TAX("template_document_tax.pdf")
    ;

    private final String resourcePath;

    PdfFileTemplate(String fileName) {
        this.resourcePath = "static/pdf/" + fileName;
    }

    public PDDocument load() throws IOException {
        return Loader.loadPDF(getRandomAccessRead());
    }

    public RandomAccessRead getRandomAccessRead() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        return new RandomAccessReadBuffer(inputStream.readAllBytes());
    }

}

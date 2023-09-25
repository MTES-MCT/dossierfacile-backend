package fr.dossierfacile.api.pdfgenerator.service.templates;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

public enum PdfFileTemplate {

    FIRST_TABLE_OF_CONTENT_PAGE("template_Dossier_PDF_first_page_1.pdf"),
    OTHER_TABLE_OF_CONTENT_PAGES("template_Dossier_PDF_first_page_2.pdf"),
    ATTACHMENTS_AND_CLARIFICATIONS("template_Dossier_PDF_attachments_and_clarification.pdf"),
    DOCUMENT_FINANCIAL("template_document_financial.pdf"),
    DOCUMENT_TAX("template_document_tax.pdf")
    ;

    private final String fileName;

    PdfFileTemplate(String fileName) {
        this.fileName = fileName;
    }

    File getFile() throws IOException {
        return new ClassPathResource("static/pdf/" + fileName).getFile();
    }

}

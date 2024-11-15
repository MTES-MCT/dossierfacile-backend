package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;

public interface PdfSignatureService {

    void signAndSave(PDDocument document, ByteArrayOutputStream baos) throws Exception;
}

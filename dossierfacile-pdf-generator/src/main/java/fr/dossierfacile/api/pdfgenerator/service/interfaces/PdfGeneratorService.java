package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.StorageFile;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface PdfGeneratorService {
    void processPdfGenerationFormWatermark(Long watermarkDocumentId);

    StorageFile generateBOPdfDocument(Long documentId) throws FileNotFoundException;

    void generateFullDossierPdf(Long apartmentSharingId) throws IOException;
}

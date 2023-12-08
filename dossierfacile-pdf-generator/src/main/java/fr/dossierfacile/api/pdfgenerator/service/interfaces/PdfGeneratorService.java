package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;

import java.io.IOException;

public interface PdfGeneratorService {
    void processPdfGenerationFormWatermark(Long watermarkDocumentId);

    StorageFile generateBOPdfDocument(Document document) throws Exception;

    void generateFullDossierPdf(Long apartmentSharingId) throws IOException;
}

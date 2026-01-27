package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.model.document_ia.DocumentIAResultModel;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentIAService {
    void saveFileAnalysis(DocumentIAResultModel payload);

    void sendForAnalysis(MultipartFile multipartFile, File file, Document document);

    void analyseDocument(Document document);

    void checkAnalysisStatus(DocumentIAFileAnalysis documentIAFileAnalysis);
}
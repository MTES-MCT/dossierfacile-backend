package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.process.file.repository.DocumentRepository;
import fr.dossierfacile.process.file.service.processors.AutomaticRulesValidationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class AnalyzeDocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentAnalysisReportRepository documentAnalysisReportRepository;
    private final AutomaticRulesValidationService automaticRulesValidationService;

    @Transactional
    public void processDocument(Long documentId) {
        Document document = documentRepository.findById(documentId).orElseThrow(IllegalStateException::new);

        if (hasBeenAnalysed(document)) {
            log.info("Ignoring document {} because it has already been analysed", documentId);
        }
        try {
            DocumentAnalysisReport report = automaticRulesValidationService.process(document);
            document.setDocumentAnalysisReport(report);
            documentAnalysisReportRepository.save(report);
            documentRepository.save(document);
        } catch(Exception e){
            log.error("Unable to build report", e);
            throw e;
        }
    }

    private boolean hasBeenAnalysed(Document document) {
        return document.getDocumentAnalysisReport() != null;
    }

}

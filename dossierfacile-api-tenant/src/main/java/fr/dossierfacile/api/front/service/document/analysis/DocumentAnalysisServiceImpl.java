package fr.dossierfacile.api.front.service.document.analysis;

import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.document.analysis.rule.AbstractRulesValidationService;
import fr.dossierfacile.api.front.service.document.analysis.rule.CarteNationalIdentiteRulesValidationService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
@Service
public class DocumentAnalysisServiceImpl {

    private final Map<DocumentSubCategory, AbstractRulesValidationService> mapOfValidators;
    private final DocumentRepository documentRepository;
    private final DocumentAnalysisReportRepository documentAnalysisReportRepository;

    @Autowired
    public DocumentAnalysisServiceImpl(
            CarteNationalIdentiteRulesValidationService carteNationalIdentiteRulesValidationService,
            DocumentRepository documentRepository, DocumentAnalysisReportRepository documentAnalysisReportRepository) {
        mapOfValidators = new EnumMap<>(DocumentSubCategory.class);
        mapOfValidators.put(DocumentSubCategory.FRENCH_IDENTITY_CARD, carteNationalIdentiteRulesValidationService);
        this.documentRepository = documentRepository;
        this.documentAnalysisReportRepository = documentAnalysisReportRepository;
    }

    public void analyseDocument(Document document) {
        var validator = mapOfValidators.get(document.getDocumentSubCategory());
        if (validator != null) {
            log.info("Analyzing document of subcategory: {}", document.getDocumentSubCategory());
            var report = getDocumentReport(document);
            report = validator.process(document, report);
            if (report.getFailedRules().isEmpty() && report.getInconclusiveRules().isEmpty()) {
                report.setAnalysisStatus(DocumentAnalysisStatus.CHECKED);
            } else if (!report.getFailedRules().isEmpty()) {
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else {
                report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
            }
            documentAnalysisReportRepository.save(report);
            document.setDocumentAnalysisReport(report);
            documentRepository.save(document);
            log.info("Report of document of subcategory: {}", document.getDocumentSubCategory());
        } else {
            log.warn("No validator found for document subcategory: {}", document.getDocumentSubCategory());
        }
    }

    private DocumentAnalysisReport getDocumentReport(Document document) {
        DocumentAnalysisReport previousReport = document.getDocumentAnalysisReport();

        // If a previous report exists, reset its fields for a new analysis
        if (previousReport != null) {
            previousReport.setFailedRules(new LinkedList<>());
            previousReport.setPassedRules(new LinkedList<>());
            previousReport.setInconclusiveRules(new LinkedList<>());
            previousReport.setCreatedAt(LocalDateTime.now());
            previousReport.setAnalysisStatus(null);
            return previousReport;
        }

        return DocumentAnalysisReport.builder()
                .document(document)
                .dataDocumentId(document.getId())
                .failedRules(new LinkedList<>())
                .passedRules(new LinkedList<>())
                .inconclusiveRules(new LinkedList<>())
                .createdAt(LocalDateTime.now())
                .build();
    }

}

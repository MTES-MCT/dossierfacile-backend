package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.document.analysis.rule.AbstractRulesValidationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class DocumentAnalysisServiceImpl {

    private final Map<DocumentSubCategory, AbstractRulesValidationService> mapOfValidators;
    private final DocumentCommonRepository documentRepository;
    private final DocumentAnalysisReportRepository documentAnalysisReportRepository;

    public void analyseDocument(Document document) {
        var validator = mapOfValidators.get(document.getDocumentSubCategory());
        if (validator != null) {
            log.info("Analyzing document of subcategory: {}", document.getDocumentSubCategory());
            var report = getDocumentReport(document);
            report = validator.process(document, report);

            boolean hasFailed = !report.getFailedRules().isEmpty();
            boolean hasInconclusive = !report.getInconclusiveRules().isEmpty();
            boolean hasPassed = !report.getPassedRules().isEmpty();

            // Empty set -> undefined
            if (!hasFailed && !hasInconclusive && !hasPassed) {
                report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
            // Any failed -> denied
            } else if (hasFailed) {
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            // No failed, but some inconclusive -> undefined
            } else if (hasInconclusive) {
                report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
            // Only passed -> checked
            } else {
                report.setAnalysisStatus(DocumentAnalysisStatus.CHECKED);
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

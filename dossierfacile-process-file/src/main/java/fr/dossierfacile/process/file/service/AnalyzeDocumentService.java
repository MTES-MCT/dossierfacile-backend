package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import fr.dossierfacile.process.file.repository.DocumentRepository;
import fr.dossierfacile.process.file.service.document_rules.AbstractRulesValidationService;
import fr.dossierfacile.process.file.service.document_rules.DocumentRulesValidationServiceFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class AnalyzeDocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentAnalysisReportRepository documentAnalysisReportRepository;
    private final DocumentRulesValidationServiceFactory documentRulesValidationServiceFactory;
    private final QueueMessageRepository queueMessageRepository;

    @Transactional
    public void processDocument(Long documentId) throws RetryableOperationException {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.info("Document {} does not exist anymore", documentId);
            return;
        }
        if (hasBeenAnalysed(document)) {
            log.info("Ignoring document {} because it has already been analysed", documentId);
            return;
        }
        // before to analyze checks if a child file analysis is currently pending/processing
        if (!readyToBeAnalysed(document)) {
            throw new RetryableOperationException("Not yet ready to be analysed");
        }

        try {
            List<AbstractRulesValidationService> rulesValidationServices = documentRulesValidationServiceFactory.getServices(document);
            if (!CollectionUtils.isEmpty(rulesValidationServices)) {
                Optional.ofNullable(document.getDocumentAnalysisReport()).ifPresent((report) -> {
                    document.setDocumentAnalysisReport(null);
                    documentAnalysisReportRepository.delete(report);
                });

                DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                        .document(document)
                        .dataDocumentId(documentId)
                        .failedRules(new LinkedList<>())
                        .passedRules(new LinkedList<>())
                        .inconclusiveRules(new LinkedList<>())
                        .createdAt(LocalDateTime.now())
                        .build();

                rulesValidationServices.forEach(rulesService -> rulesService.process(document, report));

                computeDocumentAnalysisReportStatus(report);

                document.setDocumentAnalysisReport(report);
                documentAnalysisReportRepository.save(report);
                documentRepository.save(document);// necessaire?
            }

        } catch (Exception e) {
            log.error("Unable to build report", e);
            throw e;
        }
    }

    private boolean readyToBeAnalysed(Document document) {
        // checks if a child file analysis is currently pending/processing
        List<?> messages = queueMessageRepository.findByQueueNameAndDocumentIdAndStatusIn(QueueName.QUEUE_FILE_ANALYSIS,
                document.getId(),
                List.of(QueueMessageStatus.PENDING, QueueMessageStatus.PROCESSING));
        //List<BlurryFileAnalysis> blurryAnalysis = document.getFiles().stream().map(File::getBlurryFileAnalysis).filter(Objects::nonNull).toList();
        //blurryAnalysis.forEach(blurryAnalysis1 -> log.info("Found blurry file analysis : {}", blurryAnalysis1));
        //if (blurryAnalysis.size() != document.getFiles().size()) {
        //    log.info("Document {} is not ready to be analysed because it has {} files with blurry analysis, but {} files in total",
        //            document.getId(), blurryAnalysis.size(), document.getFiles().size());
        //    return false;
        //}
        return CollectionUtils.isEmpty(messages);
    }

    private void computeDocumentAnalysisReportStatus(DocumentAnalysisReport documentAnalysisReport) {
        // This will happen if there was an exception during the analysis
        if (documentAnalysisReport.getAnalysisStatus() == DocumentAnalysisStatus.UNDEFINED) {
            return;
        }

        // If there is at least one critical failed rule, the analysis statis will be DENIED (This is temporary until we are confident with the blurry algorithms)
        var criticalFailedRules = documentAnalysisReport.getFailedRules().stream()
                .filter(rule -> rule.getLevel() == DocumentRuleLevel.CRITICAL)
                .toList();

        if (CollectionUtils.isNotEmpty(criticalFailedRules)) {
            documentAnalysisReport.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            return;
        }

        // Other wise we check if the analysis is inclusive or not
        // If there is at least one inconclusive rule, the analysis status is UNDEFINED
        if (CollectionUtils.isNotEmpty(documentAnalysisReport.getInconclusiveRules())) {
            documentAnalysisReport.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
            return;
        }

        // Other wise it means that the analysis is Checked
        documentAnalysisReport.setAnalysisStatus(DocumentAnalysisStatus.CHECKED);
    }

    private boolean hasBeenAnalysed(Document document) {
        return document.getDocumentAnalysisReport() != null;
    }

}
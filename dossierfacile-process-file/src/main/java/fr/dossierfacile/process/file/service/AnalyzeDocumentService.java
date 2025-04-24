package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import fr.dossierfacile.process.file.repository.DocumentRepository;
import fr.dossierfacile.process.file.service.documentrules.DocumentRulesValidationServiceFactory;
import fr.dossierfacile.process.file.service.documentrules.RulesValidationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
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
            List<RulesValidationService> rulesValidationServices = documentRulesValidationServiceFactory.getServices(document);
            if (!CollectionUtils.isEmpty(rulesValidationServices)) {
                Optional.ofNullable(document.getDocumentAnalysisReport()).ifPresent((report) -> {
                    document.setDocumentAnalysisReport(null);
                    documentAnalysisReportRepository.delete(report);
                });
                DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                        .document(document)
                        .brokenRules(new LinkedList<>())
                        .analysisStatus(DocumentAnalysisStatus.UNDEFINED)
                        .build();
                rulesValidationServices.forEach(rulesService -> rulesService.process(document, report));
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
        return CollectionUtils.isEmpty(messages);
    }

    private boolean hasBeenAnalysed(Document document) {
        return document.getDocumentAnalysisReport() != null;
    }

}

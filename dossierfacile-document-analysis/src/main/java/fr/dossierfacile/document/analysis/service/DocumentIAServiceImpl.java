package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.DocumentIAResultModel;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import fr.dossierfacile.document.analysis.DocumentIAConfig;
import fr.dossierfacile.document.analysis.external.documentia.DocumentIAClient;
import fr.dossierfacile.document.analysis.external.documentia.DocumentIARequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentIAServiceImpl implements DocumentIAService {

    // Document IA answers within minutes: past this delay, stop waiting and release the document.
    static final Duration ANALYSIS_TIMEOUT = Duration.ofHours(24);

    private final DocumentIAClient documentIAClient;
    private final DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;
    private final DocumentIAConfig documentIAConfig;
    private final DocumentAnalysisServiceImpl documentAnalysisService;
    private final DocumentIAResultSanitizer documentIAResultSanitizer;

    @Override
    @Transactional
    public void saveFileAnalysis(DocumentIAResultModel payload) {

        var documentIAFileAnalysis = documentIAFileAnalysisRepository.findByDocumentIaExecutionId(payload.getId());

        documentIAFileAnalysis.ifPresentOrElse(analysis -> {
            if (payload.getStatus() == DocumentIAFileAnalysisStatus.STARTED) {
                log.warn("Document IA callback with started status for id : {}", payload.getId());
                return;
            }

            if (payload.getStatus() == DocumentIAFileAnalysisStatus.SUCCESS) {
                analysis.setAnalysisStatus(DocumentIAFileAnalysisStatus.SUCCESS);
                var result = payload.getData().getResult();
                var sanitizedResult = documentIAResultSanitizer.sanitize(result, analysis.getFile().getDocument());
                analysis.setResult(sanitizedResult);
                documentIAFileAnalysisRepository.save(analysis);
            } else if (payload.getStatus() == DocumentIAFileAnalysisStatus.FAILED) {
                analysis.setAnalysisStatus(DocumentIAFileAnalysisStatus.FAILED);
                documentIAFileAnalysisRepository.save(analysis);
            } else {
                log.info("Document IA status is STARTED skip updating");
                return;
            }

            analyseParentDocument(analysis);

        }, () -> log.warn("Document IA file analysis not found with id : {}", payload.getId()));

    }

    private void analyseParentDocument(DocumentIAFileAnalysis analysis) {
        if (analysis.getFile() != null && analysis.getFile().getDocument() != null) {
            analyseDocument(analysis.getFile().getDocument());
        } else {
            log.warn("Document IA file analysis saved but file or document is null for analysis id: {}", analysis.getId());
        }
    }

    public void analyseDocument(Document document) {
        // We try to retrieve all the pending analyses for the document. If there is none, we can start processing the document. Otherwise we wait the next webhook callback!
        var analysis = document.getFiles()
                .stream()
                .map(File::getDocumentIAFileAnalysis)
                .filter(Objects::nonNull)
                .toList();

        var startedAnalysis = analysis.stream()
                .filter(it -> it.getAnalysisStatus() == DocumentIAFileAnalysisStatus.STARTED)
                .count();

        // If we have some analysis and anyone is in STARTED status we can analyze the document.
        if (!analysis.isEmpty() && startedAnalysis == 0) {
            log.warn("Start document analysis for document id: {}", document.getId());
            documentAnalysisService.analyseDocument(document);
        }
    }

    @Override
    public void sendForAnalysis(MultipartFile multipartFile, File file, Document document, long tenantId) {
        if (!documentIAConfig.hasToSendFileForAnalysis(document, tenantId)) {
            return;
        }
        var configuration = documentIAConfig.getWorkflowConfig(document);
        var request = DocumentIARequest.builder()
                .metadata("{ \"document_id\": " + document.getId() + " }")
                .file(multipartFile)
                .overrides(configuration.getOverrides())
                .build();
        try {
            var response = documentIAClient.sendForAnalysis(request, configuration.getWorkflowId());
            var analysis = DocumentIAFileAnalysis.builder()
                    .file(file)
                    .documentIaWorkflowId(response.getData().getWorkflowId())
                    .documentIaExecutionId(response.getData().getExecutionId())
                    .analysisStatus(DocumentIAFileAnalysisStatus.STARTED)
                    .build();
            documentIAFileAnalysisRepository.save(analysis);
            file.setDocumentIAFileAnalysis(analysis);
        } catch (Exception e) {
            log.error("Error sending document for analysis: {}", e.getMessage(), e);
            var analysis = DocumentIAFileAnalysis.builder()
                    .file(file)
                    .documentIaWorkflowId(configuration.getWorkflowId())
                    .analysisStatus(DocumentIAFileAnalysisStatus.FAILED)
                    .build();
            documentIAFileAnalysisRepository.save(analysis);
            file.setDocumentIAFileAnalysis(analysis);
            analyseDocument(document);
        }
    }

    @Override
    @Transactional
    public void checkAnalysisStatus(DocumentIAFileAnalysis documentIAFileAnalysis) {
        DocumentIAResultModel response;
        try {
            response = documentIAClient.checkAnalysisStatus(
                    documentIAFileAnalysis.getDocumentIaExecutionId()
            );
        } catch (Exception e) {
            // The remote analysis may still succeed: keep STARTED, the scheduler retries at its next run.
            log.warn("Unable to read Document IA execution status; will retry execution id: {} ({})",
                    documentIAFileAnalysis.getDocumentIaExecutionId(), e.toString());
            return;
        }

        // Outside the try: a local error must not mark the analysis FAILED.
        this.saveFileAnalysis(response);
    }

    @Override
    @Transactional
    public boolean failIfTimedOut(DocumentIAFileAnalysis analysis) {
        if (analysis.getCreationDate() == null
                || analysis.getCreationDate().isAfter(LocalDateTime.now().minus(ANALYSIS_TIMEOUT))) {
            return false;
        }
        // Reloaded: a callback may have completed it since the scheduler read it.
        documentIAFileAnalysisRepository.findById(analysis.getId())
                .filter(current -> current.getAnalysisStatus() == DocumentIAFileAnalysisStatus.STARTED)
                .ifPresent(current -> {
                    log.error("Document IA analysis timed out after {} hours, marking it failed. Execution id: {}",
                            ANALYSIS_TIMEOUT.toHours(), current.getDocumentIaExecutionId());
                    current.setAnalysisStatus(DocumentIAFileAnalysisStatus.FAILED);
                    documentIAFileAnalysisRepository.save(current);
                    analyseParentDocument(current);
                });
        return true;
    }
}

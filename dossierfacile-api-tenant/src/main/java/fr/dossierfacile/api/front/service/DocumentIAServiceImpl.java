package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.external.documentIA.DocumentIAClient;
import fr.dossierfacile.api.front.external.documentIA.DocumentIARequest;
import fr.dossierfacile.api.front.model.documentIA.WebhookModel;
import fr.dossierfacile.api.front.service.document.analysis.DocumentAnalysisServiceImpl;
import fr.dossierfacile.api.front.service.interfaces.DocumentIAService;
import fr.dossierfacile.common.config.DocumentIAConfig;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Component
@Slf4j
public class DocumentIAServiceImpl implements DocumentIAService {

    private final DocumentIAClient documentIAClient;
    private final DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;
    private final DocumentIAConfig documentIAConfig;
    private final DocumentAnalysisServiceImpl documentAnalysisService;


    DocumentIAServiceImpl(
            DocumentIAClient documentIAClient,
            DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository,
            DocumentIAConfig documentIAConfig,
            DocumentAnalysisServiceImpl documentAnalysisService
    ) {
        this.documentIAClient = documentIAClient;
        this.documentIAFileAnalysisRepository = documentIAFileAnalysisRepository;
        this.documentIAConfig = documentIAConfig;
        this.documentAnalysisService = documentAnalysisService;
    }

    @Override
    public void handleWebhookCallback(WebhookModel payload) {

        var documentIAFileAnalysis = documentIAFileAnalysisRepository.findByDocumentIaExecutionId(payload.getId());

        documentIAFileAnalysis.ifPresentOrElse(analysis -> {
            if (payload.getStatus() == DocumentIAFileAnalysisStatus.STARTED) {
                log.warn("Document IA callback with started status for id : {}", payload.getId());
                return;
            }

            if (payload.getStatus() == DocumentIAFileAnalysisStatus.SUCCESS) {
                analysis.setAnalysisStatus(DocumentIAFileAnalysisStatus.SUCCESS);
                analysis.setResult(payload.getData().getResult());
                documentIAFileAnalysisRepository.save(analysis);
            } else {
                analysis.setAnalysisStatus(DocumentIAFileAnalysisStatus.FAILED);
                documentIAFileAnalysisRepository.save(analysis);
            }

            analyseDocument(analysis.getFile().getDocument());
        }, () -> log.warn("Document IA file analysis not found with id : {}", payload.getId()));

    }

    public void analyseDocument(Document document) {
        // We try to retrieve all the pending analyses for the document. If there is none, we can start processing the document. Otherwise we wait the next webhook callback!
        var notFinishedAnalyses = document.getFiles()
                .stream()
                .map(File::getDocumentIAFileAnalysis)
                .filter(Objects::nonNull)
                .filter(analysis -> analysis.getAnalysisStatus() == DocumentIAFileAnalysisStatus.STARTED)
                .count();

        if (notFinishedAnalyses == 0) {
            log.warn("Start document analysis for document id: {}", document.getId());
            documentAnalysisService.analyseDocument(document);
        }
    }


    @Override
    public void sendForAnalysis(MultipartFile multipartFile, File file, Document document) {
        if (!documentIAConfig.hasToSendFileForAnalysis(document)) {
            return;
        }
        var request = DocumentIARequest.builder().metadata("{ \"document_id\": " + document.getId() + " }").file(multipartFile).build();
        try {
            var response = documentIAClient.sendForAnalysis(request);
            documentIAFileAnalysisRepository.save(
                    DocumentIAFileAnalysis.builder()
                            .file(file)
                            .documentIaWorkflowId(response.getData().getWorkflowId())
                            .documentIaExecutionId(response.getData().getExecutionId())
                            .analysisStatus(DocumentIAFileAnalysisStatus.STARTED)
                            .dataFileId(file.getId())
                            .dataDocumentId(document.getId())
                            .build()
            );
        } catch (Exception e) {
            // Log the exception or handle it as needed
            System.err.println("Error sending document for analysis: " + e.getMessage());
        }
    }
}

package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.common.config.DocumentIAConfig;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.DocumentIAResultModel;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import fr.dossierfacile.document.analysis.external.documentia.DocumentIAClient;
import fr.dossierfacile.document.analysis.external.documentia.DocumentIARequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentIAServiceImpl implements DocumentIAService {

    private final DocumentIAClient documentIAClient;
    private final DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;
    private final DocumentIAConfig documentIAConfig;
    private final DocumentAnalysisServiceImpl documentAnalysisService;


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
                analysis.setResult(payload.getData().getResult());
                documentIAFileAnalysisRepository.save(analysis);
            } else if (payload.getStatus() == DocumentIAFileAnalysisStatus.FAILED) {
                analysis.setAnalysisStatus(DocumentIAFileAnalysisStatus.FAILED);
                documentIAFileAnalysisRepository.save(analysis);
            } else {
                log.info("Document IA status is STARTED skip updating");
                return;
            }

            if (analysis.getFile() != null && analysis.getFile().getDocument() != null) {
                analyseDocument(analysis.getFile().getDocument());
            } else {
                log.warn("Document IA file analysis saved but file or document is null for analysis id: {}", analysis.getId());
            }

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
            log.error("Error sending document for analysis: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void checkAnalysisStatus(DocumentIAFileAnalysis documentIAFileAnalysis) {
        try {
            var response = documentIAClient.checkAnalysisStatus(
                    documentIAFileAnalysis.getDocumentIaExecutionId()
            );

            this.saveFileAnalysis(response);
        } catch (Exception e) {
            log.error("Error checking analysis status for Document IA execution id: {}", documentIAFileAnalysis.getDocumentIaExecutionId(), e);
            documentIAFileAnalysis.setAnalysisStatus(DocumentIAFileAnalysisStatus.FAILED);
            documentIAFileAnalysisRepository.save(documentIAFileAnalysis);
        }
    }
}

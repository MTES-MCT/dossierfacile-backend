package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.external.documentIA.DocumentIAClient;
import fr.dossierfacile.api.front.external.documentIA.DocumentIARequest;
import fr.dossierfacile.api.front.model.documentIA.WebhookModel;
import fr.dossierfacile.api.front.service.interfaces.DocumentIAService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class DocumentIAServiceImpl implements DocumentIAService {

    private final DocumentIAClient documentIAClient;
    private final DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;


    DocumentIAServiceImpl(
            DocumentIAClient documentIAClient,
            DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository
    ) {
        this.documentIAClient = documentIAClient;
        this.documentIAFileAnalysisRepository = documentIAFileAnalysisRepository;
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
        }, () -> log.warn("Document IA file analysis not found with id : {}", payload.getId()));

    }


    @Override
    public void sendForAnalysis(MultipartFile multipartFile, File file, Document document) {
        if (!hasToSendFileForAnalysis(document)) {
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

    private boolean hasToSendFileForAnalysis(Document document) {
        return
                document.getDocumentSubCategory() == DocumentSubCategory.FRENCH_IDENTITY_CARD ||
                document.getDocumentSubCategory() == DocumentSubCategory.MY_NAME ||
                document.getDocumentSubCategory() == DocumentSubCategory.DRIVERS_LICENSE ||
                document.getDocumentSubCategory() == DocumentSubCategory.FRENCH_PASSPORT ||
                document.getDocumentSubCategory() == DocumentSubCategory.SALARY;
    }
}

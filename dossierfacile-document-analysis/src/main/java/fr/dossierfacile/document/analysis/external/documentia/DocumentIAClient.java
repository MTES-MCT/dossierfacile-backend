package fr.dossierfacile.document.analysis.external.documentia;

import fr.dossierfacile.common.model.document_ia.DocumentIAResultModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class DocumentIAClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String workflowId;

    public DocumentIAClient(
            RestTemplate restTemplate,
            @Value("${document.ia.api.base.url}") String baseUrl,
            @Value("${document.ia.api.key}") String apiKey,
            @Value("${document.ia.api.workflow.id}") String workflowId
    ) {

        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.workflowId = workflowId;
    }

    public DocumentIAResponse sendForAnalysis(DocumentIARequest request) {
        String url = String.format("%s/workflows/%s/execute", baseUrl, workflowId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Api-Key", apiKey);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(createMultipartRequest(request), headers);

        ResponseEntity<DocumentIAResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                DocumentIAResponse.class
        );

        return response.getBody();
    }

    public DocumentIAResultModel checkAnalysisStatus(String executionId) {
        String url = String.format("%s/executions/%s", baseUrl, executionId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<DocumentIAResultModel> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                DocumentIAResultModel.class
        );

        return response.getBody();
    }


    private MultiValueMap<String, Object> createMultipartRequest(DocumentIARequest request) {
        MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();

        multiValueMap.add("metadata", request.getMetadata());
        multiValueMap.add("file", request.getFile().getResource());

        return multiValueMap;
    }

}

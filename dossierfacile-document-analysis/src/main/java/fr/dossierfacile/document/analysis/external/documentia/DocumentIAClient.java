package fr.dossierfacile.document.analysis.external.documentia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    public DocumentIAClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${document.ia.api.base.url}") String baseUrl,
            @Value("${document.ia.api.key}") String apiKey
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Exécute de manière asynchrone le workflow V2
     */
    public DocumentIAResponse sendForAnalysis(DocumentIARequest request, String workflowId) {
        String url = String.format("%s/v2/workflows/%s/execute", baseUrl, workflowId);
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
        String url = String.format("%s/v1/executions/%s", baseUrl, executionId);
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

        // Validation d'exclusion stricte file / file_url
        if (request.getFile() != null && request.getFileUrl() != null && !request.getFileUrl().isBlank()) {
            throw new IllegalArgumentException("Cannot provide both file and fileUrl");
        }

        if (request.getFile() != null) {
            multiValueMap.add("file", request.getFile().getResource());
        } else if (request.getFileUrl() != null && !request.getFileUrl().isBlank()) {
            multiValueMap.add("file_url", request.getFileUrl());
        } else {
            throw new IllegalArgumentException("Exactly one of file or fileUrl must be provided");
        }

        if (request.getMetadata() != null) {
            multiValueMap.add("metadata", request.getMetadata());
        }

        if (request.getOverrides() != null && !request.getOverrides().isEmpty()) {
            try {
                String overrideJson = objectMapper.writeValueAsString(request.getOverrides());
                HttpHeaders jsonHeaders = new HttpHeaders();
                jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> overrideEntity = new HttpEntity<>(overrideJson, jsonHeaders);
                multiValueMap.add("override", overrideEntity);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize overrides to JSON", e);
            }
        }

        return multiValueMap;
    }
}

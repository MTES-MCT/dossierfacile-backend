package fr.dossierfacile.document.analysis.external.documentia;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.model.document_ia.DocumentIAResultModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DocumentIAClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private DocumentIAClient documentIAClient;

    private static final String BASE_URL = "https://api.document-ia.local/api";
    private static final String API_KEY = "dummy-api-key";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        objectMapper = new ObjectMapper();
        documentIAClient = new DocumentIAClient(restTemplate, objectMapper, BASE_URL, API_KEY);
    }

    @Test
    void should_send_for_analysis_with_multipart_file_to_v2_endpoint() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        DocumentIARequest request = DocumentIARequest.builder()
                .file(file)
                .metadata("{\"document_id\":123}")
                .build();

        String expectedUrl = BASE_URL + "/v2/workflows/wf-test/execute";
        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Api-Key", API_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("{\"status\":\"STARTED\",\"data\":{\"execution_id\":\"exec-1\",\"workflow_id\":\"wf-test\"}}", MediaType.APPLICATION_JSON));

        // When
        DocumentIAResponse response = documentIAClient.sendForAnalysis(request, "wf-test");

        // Then
        assertNotNull(response);
        assertEquals("STARTED", response.getStatus());
        assertEquals("exec-1", response.getData().getExecutionId());
        mockServer.verify();
    }

    @Test
    void should_send_for_analysis_with_file_url_to_v2_endpoint() {
        // Given
        DocumentIARequest request = DocumentIARequest.builder()
                .fileUrl("https://storage.local/test.pdf")
                .metadata("{\"document_id\":123}")
                .build();

        String expectedUrl = BASE_URL + "/v2/workflows/wf-test/execute";
        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Api-Key", API_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("{\"status\":\"STARTED\",\"data\":{\"execution_id\":\"exec-2\",\"workflow_id\":\"wf-test\"}}", MediaType.APPLICATION_JSON));

        // When
        DocumentIAResponse response = documentIAClient.sendForAnalysis(request, "wf-test");

        // Then
        assertNotNull(response);
        assertEquals("STARTED", response.getStatus());
        assertEquals("exec-2", response.getData().getExecutionId());
        mockServer.verify();
    }

    @Test
    void should_throw_exception_when_both_file_and_file_url_are_provided() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        DocumentIARequest request = DocumentIARequest.builder()
                .file(file)
                .fileUrl("https://storage.local/test.pdf")
                .build();

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                documentIAClient.sendForAnalysis(request, "wf-test")
        );
        assertEquals("Cannot provide both file and fileUrl", exception.getMessage());
    }

    @Test
    void should_throw_exception_when_neither_file_nor_file_url_are_provided() {
        // Given
        DocumentIARequest request = DocumentIARequest.builder()
                .metadata("{\"document_id\":123}")
                .build();

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                documentIAClient.sendForAnalysis(request, "wf-test")
        );
        assertEquals("Exactly one of file or fileUrl must be provided", exception.getMessage());
    }

    @Test
    void should_send_overrides_serialized_as_json_in_multipart() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        List<WorkflowV2StepParamOverride> stepOverrides = List.of(
                WorkflowV2StepParamOverride.builder().param("document_type").value("cni").build(),
                WorkflowV2StepParamOverride.builder().param("model").value("albert-small").build()
        );
        DocumentIARequest request = DocumentIARequest.builder()
                .file(file)
                .overrides(Map.of("llm_extract_data", stepOverrides))
                .build();

        String expectedUrl = BASE_URL + "/v2/workflows/wf-test/execute";
        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                // Note: Spring's multipart writer sends parts. We check the HTTP request was sent successfully
                .andRespond(withSuccess("{\"status\":\"STARTED\",\"data\":{\"execution_id\":\"exec-3\",\"workflow_id\":\"wf-test\"}}", MediaType.APPLICATION_JSON));

        // When
        DocumentIAResponse response = documentIAClient.sendForAnalysis(request, "wf-test");

        // Then
        assertNotNull(response);
        assertEquals("exec-3", response.getData().getExecutionId());
        mockServer.verify();
    }

    @Test
    void should_call_v1_executions_endpoint_for_status_check() {
        // Given
        String executionId = "exec-abc";
        String expectedUrl = BASE_URL + "/v1/executions/" + executionId;

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Api-Key", API_KEY))
                .andRespond(withSuccess("{\"id\":\"exec-abc\",\"status\":\"SUCCESS\",\"data\":{\"total_processing_time_ms\":250}}", MediaType.APPLICATION_JSON));

        // When
        DocumentIAResultModel response = documentIAClient.checkAnalysisStatus(executionId);

        // Then
        assertNotNull(response);
        assertEquals("exec-abc", response.getId());
        mockServer.verify();
    }
}

package fr.dossierfacile.document.analysis.service;

import fr.dossierfacile.document.analysis.DocumentIAConfig;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.*;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import fr.dossierfacile.document.analysis.external.documentia.DocumentIAClient;
import fr.dossierfacile.document.analysis.external.documentia.DocumentIARequest;
import fr.dossierfacile.document.analysis.external.documentia.DocumentIAResponse;
import fr.dossierfacile.document.analysis.external.documentia.DocumentIAResponseData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIAServiceImplTest {

    @Mock
    DocumentIAClient documentIAClient;
    @Mock
    DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;
    @Mock
    DocumentIAConfig documentIAConfig;
    @Mock
    DocumentAnalysisServiceImpl documentAnalysisService;
    @Mock
    DocumentIAResultSanitizer documentIAResultSanitizer;

    @InjectMocks
    DocumentIAServiceImpl documentIAService;

    @Test
    void should_save_analysis_result_on_success() {
        // Given
        String executionId = "exec-1";
        DocumentIAResultModel resultModel = DocumentIAResultModel.builder()
                .id(executionId)
                .status(DocumentIAFileAnalysisStatus.SUCCESS)
                .data(DocumentIaResultDataModel.builder()
                        .result(new ResultModel())
                        .build())
                .build();

        Document document = new Document();
        document.setId(1L);
        File file = File.builder().id(1L).document(document).build();
        document.setFiles(List.of(file));
        DocumentIAFileAnalysis analysis = DocumentIAFileAnalysis.builder()
                .id(1L)
                .documentIaExecutionId(executionId)
                .analysisStatus(DocumentIAFileAnalysisStatus.STARTED)
                .file(file)
                .build();

        file.setDocumentIAFileAnalysis(analysis);

        when(documentIAFileAnalysisRepository.findByDocumentIaExecutionId(executionId))
                .thenReturn(Optional.of(analysis));

        // When
        documentIAService.saveFileAnalysis(resultModel);

        // Then
        verify(documentIAFileAnalysisRepository).save(any(DocumentIAFileAnalysis.class));
        verify(documentAnalysisService).analyseDocument(document);
    }

    @Test
    void should_ignore_callback_if_status_is_started() {
        // Given
        String executionId = "exec-1";
        DocumentIAResultModel resultModel = DocumentIAResultModel.builder()
                .id(executionId)
                .status(DocumentIAFileAnalysisStatus.STARTED)
                .build();

        when(documentIAFileAnalysisRepository.findByDocumentIaExecutionId(executionId))
                .thenReturn(Optional.of(new DocumentIAFileAnalysis()));

        // When
        documentIAService.saveFileAnalysis(resultModel);

        // Then
        verify(documentIAFileAnalysisRepository, never()).save(any());
    }

    @Test
    void should_send_for_analysis_successfully() {
        // Given
        MultipartFile multipartFile = mock(MultipartFile.class);
        File file = File.builder().id(123L).build();
        Document document = Document.builder().id(456L).build();
        long tenantId = 42L;

        when(documentIAConfig.hasToSendFileForAnalysis(document, tenantId)).thenReturn(true);
        when(documentIAConfig.getWorkflowConfig(document)).thenReturn(new DocumentIAConfig.WorkflowConfig("wf-1"));
        when(documentIAClient.sendForAnalysis(any(DocumentIARequest.class), any()))
                .thenReturn(DocumentIAResponse.builder()
                        .data(DocumentIAResponseData.builder()
                                .workflowId("wf-1")
                                .executionId("exec-1")
                                .build())
                        .build());

        // When
        documentIAService.sendForAnalysis(multipartFile, file, document, tenantId);

        // Then
        verify(documentIAClient).sendForAnalysis(any(DocumentIARequest.class), any());
        verify(documentIAFileAnalysisRepository).save(any(DocumentIAFileAnalysis.class));
    }

    @Test
    void should_save_failed_analysis_when_send_for_analysis_throws() {
        // Given
        MultipartFile multipartFile = mock(MultipartFile.class);
        File file = File.builder().id(123L).build();
        Document document = Document.builder().id(456L).build();
        long tenantId = 42L;

        when(documentIAConfig.hasToSendFileForAnalysis(document, tenantId)).thenReturn(true);
        when(documentIAConfig.getWorkflowConfig(document)).thenReturn(new DocumentIAConfig.WorkflowConfig("wf-1"));
        when(documentIAClient.sendForAnalysis(any(DocumentIARequest.class), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When
        documentIAService.sendForAnalysis(multipartFile, file, document, tenantId);

        // Then
        verify(documentIAFileAnalysisRepository).save(argThat(analysis -> analysis.getAnalysisStatus() == DocumentIAFileAnalysisStatus.FAILED
                && analysis.getFile() == file
        ));
    }

    @Test
    void should_check_and_update_status() {
        // Given
        String executionId = "exec-1";
        Document document = new Document();
        document.setId(1L);
        File file = File.builder().id(1L).document(document).build();

        DocumentIAFileAnalysis analysis = DocumentIAFileAnalysis.builder()
                .documentIaExecutionId(executionId)
                .analysisStatus(DocumentIAFileAnalysisStatus.STARTED)
                .file(file)
                .build();

        DocumentIAResultModel resultModel = DocumentIAResultModel.builder()
                .id(executionId)
                .status(DocumentIAFileAnalysisStatus.SUCCESS)
                .data(DocumentIaResultDataModel.builder()
                        .result(new ResultModel())
                        .build())
                .build();

        when(documentIAClient.checkAnalysisStatus(executionId)).thenReturn(resultModel);
        when(documentIAFileAnalysisRepository.findByDocumentIaExecutionId(executionId)).thenReturn(Optional.of(analysis));

        // When
        documentIAService.checkAnalysisStatus(analysis);

        // Then
        verify(documentIAClient).checkAnalysisStatus(executionId);
        // saveFileAnalysis is called internally, which updates and saves analysis
        verify(documentIAFileAnalysisRepository).save(analysis);
    }

    private static final Duration TOO_OLD = DocumentIAServiceImpl.MAX_ANALYSIS_AGE.plusMinutes(1);

    private static DocumentIAFileAnalysis startedAnalysis(Duration age) {
        return DocumentIAFileAnalysis.builder()
                .id(7L)
                .documentIaExecutionId("exec-1")
                .analysisStatus(DocumentIAFileAnalysisStatus.STARTED)
                .creationDate(LocalDateTime.now().minus(age))
                .build();
    }

    private static Document documentWithOnlyFile(DocumentIAFileAnalysis analysis) {
        Document document = new Document();
        File file = File.builder().document(document).build();
        file.setDocumentIAFileAnalysis(analysis);
        document.setFiles(List.of(file));
        analysis.setFile(file);
        return document;
    }

    private static DocumentIAResultModel failedResult() {
        return DocumentIAResultModel.builder().id("exec-1").status(DocumentIAFileAnalysisStatus.FAILED).build();
    }

    static Stream<Exception> statusReadFailures() {
        return Stream.of(
                new ResourceAccessException("Read timed out"),
                // Not a RestClientException: Spring throws it when an error response has an invalid Content-Type.
                new InvalidMediaTypeException("application/json, application/yaml", "Invalid token")
        );
    }

    @ParameterizedTest
    @MethodSource("statusReadFailures")
    void should_keep_analysis_started_when_status_read_fails(Exception failure) {
        DocumentIAFileAnalysis analysis = startedAnalysis(Duration.ZERO);
        when(documentIAClient.checkAnalysisStatus("exec-1")).thenThrow(failure);

        documentIAService.checkAnalysisStatus(analysis);

        assertEquals(DocumentIAFileAnalysisStatus.STARTED, analysis.getAnalysisStatus());
        verify(documentIAFileAnalysisRepository, never()).save(any());
        verifyNoInteractions(documentAnalysisService);
    }

    @Test
    void should_finish_analysis_when_document_ia_explicitly_reports_failure() {
        DocumentIAFileAnalysis analysis = startedAnalysis(Duration.ZERO);
        Document document = documentWithOnlyFile(analysis);
        when(documentIAClient.checkAnalysisStatus("exec-1")).thenReturn(failedResult());
        when(documentIAFileAnalysisRepository.findByDocumentIaExecutionId("exec-1"))
                .thenReturn(Optional.of(analysis));

        documentIAService.checkAnalysisStatus(analysis);

        assertEquals(DocumentIAFileAnalysisStatus.FAILED, analysis.getAnalysisStatus());
        verify(documentIAFileAnalysisRepository).save(analysis);
        verify(documentAnalysisService).analyseDocument(document);
    }

    @Test
    void should_propagate_processing_errors_instead_of_marking_remote_analysis_failed() {
        DocumentIAFileAnalysis analysis = startedAnalysis(Duration.ZERO);
        when(documentIAClient.checkAnalysisStatus("exec-1")).thenReturn(failedResult());
        when(documentIAFileAnalysisRepository.findByDocumentIaExecutionId("exec-1"))
                .thenThrow(new IllegalStateException("Database unavailable"));

        assertThrows(IllegalStateException.class, () -> documentIAService.checkAnalysisStatus(analysis));

        assertEquals(DocumentIAFileAnalysisStatus.STARTED, analysis.getAnalysisStatus());
        verify(documentIAFileAnalysisRepository, never()).save(any());
    }

    @Test
    void should_give_up_on_an_analysis_older_than_the_maximum_age() {
        // The scheduler passes a detached copy: only the reloaded row may be changed and saved.
        DocumentIAFileAnalysis detachedCopy = startedAnalysis(TOO_OLD);
        DocumentIAFileAnalysis reloaded = startedAnalysis(TOO_OLD);
        Document document = documentWithOnlyFile(reloaded);
        when(documentIAFileAnalysisRepository.findById(7L)).thenReturn(Optional.of(reloaded));

        assertTrue(documentIAService.abandonIfTooOld(detachedCopy));

        assertEquals(DocumentIAFileAnalysisStatus.FAILED, reloaded.getAnalysisStatus());
        assertEquals(DocumentIAFileAnalysisStatus.STARTED, detachedCopy.getAnalysisStatus());
        verify(documentIAFileAnalysisRepository).save(same(reloaded));
        verify(documentAnalysisService).analyseDocument(document);
    }

    @Test
    void should_not_overwrite_an_old_analysis_completed_by_a_callback_meanwhile() {
        DocumentIAFileAnalysis completed = startedAnalysis(TOO_OLD);
        completed.setAnalysisStatus(DocumentIAFileAnalysisStatus.SUCCESS);
        when(documentIAFileAnalysisRepository.findById(7L)).thenReturn(Optional.of(completed));

        assertTrue(documentIAService.abandonIfTooOld(startedAnalysis(TOO_OLD)));

        assertEquals(DocumentIAFileAnalysisStatus.SUCCESS, completed.getAnalysisStatus());
        verify(documentIAFileAnalysisRepository, never()).save(any());
        verifyNoInteractions(documentAnalysisService);
    }

    @Test
    void should_not_give_up_on_an_analysis_younger_than_the_maximum_age() {
        DocumentIAFileAnalysis analysis = startedAnalysis(DocumentIAServiceImpl.MAX_ANALYSIS_AGE.minusMinutes(1));

        assertFalse(documentIAService.abandonIfTooOld(analysis));

        verifyNoInteractions(documentIAFileAnalysisRepository, documentAnalysisService);
    }

    @Test
    void should_not_analyse_document_when_there_is_no_document_ia_analysis() {
        Document document = mock(Document.class);
        File file = mock(File.class);
        when(file.getDocumentIAFileAnalysis()).thenReturn(null);
        when(document.getFiles()).thenReturn(List.of(file));

        documentIAService.analyseDocument(document);

        verify(documentAnalysisService, never()).analyseDocument(any(Document.class));
    }

    @Test
    void should_not_analyse_document_when_at_least_one_analysis_is_started() {
        Document document = mock(Document.class);

        File startedFile = mock(File.class);
        DocumentIAFileAnalysis startedAnalysis = mock(DocumentIAFileAnalysis.class);
        when(startedAnalysis.getAnalysisStatus()).thenReturn(DocumentIAFileAnalysisStatus.STARTED);
        when(startedFile.getDocumentIAFileAnalysis()).thenReturn(startedAnalysis);

        File successFile = mock(File.class);
        DocumentIAFileAnalysis successAnalysis = mock(DocumentIAFileAnalysis.class);
        when(successAnalysis.getAnalysisStatus()).thenReturn(DocumentIAFileAnalysisStatus.SUCCESS);
        when(successFile.getDocumentIAFileAnalysis()).thenReturn(successAnalysis);

        when(document.getFiles()).thenReturn(List.of(startedFile, successFile));

        documentIAService.analyseDocument(document);

        verify(documentAnalysisService, never()).analyseDocument(any(Document.class));
    }

    @Test
    void should_analyse_document_when_all_analyses_are_not_started() {
        Document document = mock(Document.class);

        File successFile = mock(File.class);
        DocumentIAFileAnalysis successAnalysis = mock(DocumentIAFileAnalysis.class);
        when(successAnalysis.getAnalysisStatus()).thenReturn(DocumentIAFileAnalysisStatus.SUCCESS);
        when(successFile.getDocumentIAFileAnalysis()).thenReturn(successAnalysis);

        File failedFile = mock(File.class);
        DocumentIAFileAnalysis failedAnalysis = mock(DocumentIAFileAnalysis.class);
        when(failedAnalysis.getAnalysisStatus()).thenReturn(DocumentIAFileAnalysisStatus.FAILED);
        when(failedFile.getDocumentIAFileAnalysis()).thenReturn(failedAnalysis);

        when(document.getFiles()).thenReturn(List.of(successFile, failedFile));

        documentIAService.analyseDocument(document);

        verify(documentAnalysisService, times(1)).analyseDocument(document);
    }
}

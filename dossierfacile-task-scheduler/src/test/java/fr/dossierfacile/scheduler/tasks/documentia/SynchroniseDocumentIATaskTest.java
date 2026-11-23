package fr.dossierfacile.scheduler.tasks.documentia;

import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import fr.dossierfacile.logging.task.LogAggregator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class SynchroniseDocumentIATaskTest {

    @Mock
    DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;
    @Mock
    DocumentIAService documentIaService;

    SynchroniseDocumentIATask synchroniseDocumentIaTask;

    @BeforeEach
    void setUp() {
        // Created by hand to override the @Lookup method
        synchroniseDocumentIaTask = new SynchroniseDocumentIATask(documentIAFileAnalysisRepository, documentIaService) {
            @Override
            protected LogAggregator logAggregator() {
                return mock(LogAggregator.class);
            }
        };
    }

    private void givenStartedAnalyses(DocumentIAFileAnalysis... analyses) {
        when(documentIAFileAnalysisRepository.findAllByAnalysisStatus(any(), any(Pageable.class)))
                .thenReturn(List.of(analyses));
    }

    private static DocumentIAFileAnalysis analysis(long id) {
        return DocumentIAFileAnalysis.builder().id(id).build();
    }

    @Test
    void should_synchronize_maximum_10_analysis_at_same_time() {
        // Given
        int totalAnalyses = 100;
        givenStartedAnalyses(IntStream.range(0, totalAnalyses)
                .mapToObj(SynchroniseDocumentIATaskTest::analysis)
                .toArray(DocumentIAFileAnalysis[]::new));

        AtomicInteger concurrentCounter = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        doAnswer(invocation -> {
            int current = concurrentCounter.incrementAndGet();
            maxConcurrent.accumulateAndGet(current, Math::max);

            // Simulate processing time
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            concurrentCounter.decrementAndGet();
            return null;
        }).when(documentIaService).checkAnalysisStatus(any());

        // When
        synchroniseDocumentIaTask.synchroniseDocumentIA();

        // Then
        assertThat(maxConcurrent.get()).as("Max concurrent executions should not exceed limit").isLessThanOrEqualTo(10);

        // Ensure all were processed
        verify(documentIaService, times(totalAnalyses)).checkAnalysisStatus(any());
    }

    // The stubs below match any analysis and compare references: the analyses are checked in parallel,
    // in no fixed order, and a stub for one analysis only would fail on the call for the other one.

    @Test
    void should_log_a_failed_synchronisation_and_continue_with_the_other_analyses(CapturedOutput output) {
        DocumentIAFileAnalysis failing = analysis(1L);
        DocumentIAFileAnalysis healthy = analysis(2L);
        givenStartedAnalyses(failing, healthy);
        doAnswer(invocation -> {
            if (invocation.getArgument(0) == failing) {
                throw new IllegalStateException("Database unavailable");
            }
            return null;
        }).when(documentIaService).checkAnalysisStatus(any());

        synchroniseDocumentIaTask.synchroniseDocumentIA();

        verify(documentIaService).checkAnalysisStatus(healthy);
        // The exception itself must be logged, not only the fact that something failed.
        assertThat(output).contains("analysis id=1", "IllegalStateException: Database unavailable");
    }

    @Test
    void should_not_check_status_of_abandoned_analyses() {
        DocumentIAFileAnalysis expired = analysis(1L);
        DocumentIAFileAnalysis recent = analysis(2L);
        givenStartedAnalyses(expired, recent);
        when(documentIaService.abandonIfTooOld(any())).thenAnswer(invocation -> invocation.getArgument(0) == expired);

        synchroniseDocumentIaTask.synchroniseDocumentIA();

        verify(documentIaService, never()).checkAnalysisStatus(expired);
        verify(documentIaService).checkAnalysisStatus(recent);
    }

    @Test
    void should_keep_checking_the_others_when_abandoning_an_analysis_fails() {
        DocumentIAFileAnalysis broken = analysis(1L);
        DocumentIAFileAnalysis healthy = analysis(2L);
        givenStartedAnalyses(broken, healthy);
        when(documentIaService.abandonIfTooOld(any())).thenAnswer(invocation -> {
            if (invocation.getArgument(0) == broken) {
                throw new IllegalStateException("Database unavailable");
            }
            return false;
        });

        synchroniseDocumentIaTask.synchroniseDocumentIA();

        verify(documentIaService, never()).checkAnalysisStatus(broken);
        verify(documentIaService).checkAnalysisStatus(healthy);
    }
}

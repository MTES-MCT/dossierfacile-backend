package fr.dossierfacile.scheduler.tasks.documentia;

import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import fr.dossierfacile.logging.task.LogAggregator;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SynchroniseDocumentIATaskTest {

    @Mock
    DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;
    @Mock
    DocumentIAService documentIaService;

    // Remove @InjectMocks
    SynchroniseDocumentIATask synchroniseDocumentIaTask;

    @Test
    void should_synchronize_maximum_10_analysis_at_same_time() {
        // Initialize the task manually to override the @Lookup method
        synchroniseDocumentIaTask = new SynchroniseDocumentIATask(documentIAFileAnalysisRepository, documentIaService) {
            @Override
            protected LogAggregator logAggregator() {
                return mock(LogAggregator.class);
            }
        };

        // Given
        int totalAnalyses = 100;
        List<DocumentIAFileAnalysis> analyses = IntStream.range(0, totalAnalyses)
                .mapToObj(i -> DocumentIAFileAnalysis.builder().id((long) i).build())
                .toList();

        // Assuming the repository returns a List directly
        when(documentIAFileAnalysisRepository.findAllByAnalysisStatus(any(), any(Pageable.class)))
                .thenReturn(analyses);

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
}

package fr.dossierfacile.process.file.amqp;

import fr.dossierfacile.process.file.service.AnalyzeFile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
class AnalyzeFileReceiverTest {

    private final AnalyzeFile analyzeFile = mock(AnalyzeFile.class);
    private final AnalyzeFileReceiver analyzeFileReceiver = new AnalyzeFileReceiver(analyzeFile);
    {
        analyzeFileReceiver.setAnalysisTimeoutInSeconds(1L);
    }

    private int finishedAnalysis = 0;

    @Test
    @Timeout(5)
    void should_timeout_if_analysis_takes_too_long() throws InterruptedException, ExecutionException {
        sleepSeconds(10).when(analyzeFile).processFile(1L); // Should time out
        sleepSeconds(0).when(analyzeFile).processFile(2L); // Should not time out

        long startTime = System.currentTimeMillis();
        analyzeFileReceiver.receiveFile(withId("1"));
        analyzeFileReceiver.receiveFile(withId("2"));
        analyzeFileReceiver.receiveFile(withId("1"));
        analyzeFileReceiver.receiveFile(withId("2"));
        analyzeFileReceiver.receiveFile(withId("1"));
        long executionTime = System.currentTimeMillis() - startTime;

        verify(analyzeFile, times(3)).processFile(1L);
        verify(analyzeFile, times(2)).processFile(2L);

        assertThat(executionTime).isLessThan(4000);
        assertThat(finishedAnalysis).isEqualTo(2);
    }

    @Test
    void should_fail_on_analysis_error() {
        doThrow(new RuntimeException("error!")).when(analyzeFile).processFile(anyLong());

        assertThatThrownBy(() -> analyzeFileReceiver.receiveFile(withId("1")))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("error!");
    }

    private Stubber sleepSeconds(int seconds) {
        return doAnswer((Answer<Void>) invocation -> {
            Thread.sleep(seconds * 1000L);
            finishedAnalysis++;
            return null;
        });
    }

    private static Map<String, String> withId(String number) {
        return Map.of("id", number);
    }

}
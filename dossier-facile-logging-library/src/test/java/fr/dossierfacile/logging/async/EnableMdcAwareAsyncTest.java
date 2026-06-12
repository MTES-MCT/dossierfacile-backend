package fr.dossierfacile.logging.async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class EnableMdcAwareAsyncTest {

    @Configuration
    @EnableMdcAwareAsync
    static class TestConfig {
        @Bean
        AsyncProbe asyncProbe() {
            return new AsyncProbe();
        }
    }

    static class AsyncProbe {
        @Async
        CompletableFuture<String> readMdcAndThread() {
            return CompletableFuture.completedFuture(
                    MDC.get("request_id") + "|" + Thread.currentThread().getName());
        }
    }

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void mdc_is_propagated_to_async_thread_and_executor_is_used()
            throws ExecutionException, InterruptedException, TimeoutException {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            MDC.put("request_id", "test-request-id");

            String result = context.getBean(AsyncProbe.class)
                    .readMdcAndThread()
                    .get(5, TimeUnit.SECONDS);

            assertThat(result).startsWith("test-request-id|");
            assertThat(result).contains("df-async-");
        }
    }
}

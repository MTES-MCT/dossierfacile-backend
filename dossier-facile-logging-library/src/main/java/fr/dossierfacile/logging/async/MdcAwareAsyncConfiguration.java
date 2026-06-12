package fr.dossierfacile.logging.async;

import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Provides the {@code @Async} executor used across DossierFacile web modules.
 * The executor is wired with {@link MdcTaskDecorator} so the logging context
 * (request_id / process_id) is propagated to async worker threads, which keeps
 * asynchronous logs correlated in the log aggregator.
 * <p>
 * This class is not a stereotyped component on purpose: it is only registered
 * through {@link EnableMdcAwareAsync} (via {@code @Import}), so it never leaks
 * into modules that merely have it on their classpath.
 */
public class MdcAwareAsyncConfiguration implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("df-async-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }
}

package fr.dossierfacile.logging.async;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Propagates the MDC context (notably the request_id / process_id used by the log aggregator)
 * from the calling thread to the {@code @Async} worker thread, then clears it afterwards
 * to avoid leaking context between pooled threads.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> currentMDContext = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (currentMDContext != null) {
                    MDC.setContextMap(currentMDContext);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

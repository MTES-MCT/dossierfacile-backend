package fr.dossierfacile.api.front.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class AsyncTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> currentMDContext = MDC.getCopyOfContextMap();
        return () -> {
            try {
                MDC.setContextMap(currentMDContext);
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
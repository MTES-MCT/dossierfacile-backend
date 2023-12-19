package fr.dossierfacile.process.file.amqp;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;

import static java.lang.Long.parseLong;

@Slf4j
public class LoggingContext {

    private static final String FILE = "file_id";
    private static final String ACTION = "action";
    private static final String EXECUTION_START = "execution_start";
    private static final String EXECUTION_DURATION = "execution_duration";

    static void startProcessing(Long fileId, ActionType actionType) {
        MDC.put(FILE, String.valueOf(fileId));
        MDC.put(ACTION, actionType.name());
        MDC.put(EXECUTION_START, String.valueOf(System.currentTimeMillis()));
        log.info("Received " + actionType.name() + " to process");
    }

    static void endProcessing() {
        long duration = System.currentTimeMillis() - parseLong(MDC.get(EXECUTION_START));
        MDC.put(EXECUTION_DURATION, String.valueOf(duration));
        log.info("Ending processing");
        MDC.clear();
    }

    public static Runnable copyContextInThread(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                MDC.setContextMap(contextMap);
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

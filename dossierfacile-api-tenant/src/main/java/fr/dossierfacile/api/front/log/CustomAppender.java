package fr.dossierfacile.api.front.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Context;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomAppender extends AppenderBase<ILoggingEvent> {
    private static final Map<String, List<LogModel>> logsByRequestId = new ConcurrentHashMap<>();

    @Override
    protected void append(ILoggingEvent eventObject) {
        String requestId = MDC.get("request_id");
        if (requestId != null) {
            String message = eventObject.getFormattedMessage();

            if (eventObject.getLevel().isGreaterOrEqual(Level.ERROR)) {
                IThrowableProxy throwableProxy = eventObject.getThrowableProxy();
                if (throwableProxy != null) {
                    StringBuilder stackTrace = new StringBuilder();
                    for (StackTraceElementProxy line : throwableProxy.getStackTraceElementProxyArray()) {
                        stackTrace.append(line).append("\n");
                    }
                    message += "\n" + stackTrace;
                }
            }

            LogModel log = new LogModel(message, eventObject.getLevel());
            logsByRequestId.computeIfAbsent(requestId, id -> new ArrayList<>())
                    .add(log);
        }
    }

    public static List<LogModel> getLogsForRequest(String requestId) {
        return logsByRequestId.getOrDefault(requestId, List.of());
    }

    public static void clearLogsForRequest(String requestId) {
        logsByRequestId.remove(requestId);
    }

    public static void attachToRootLogger() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        CustomAppender customAppender = new CustomAppender();
        customAppender.setName("CUSTOM");
        Context context = rootLogger.getLoggerContext();
        customAppender.setContext(context);
        customAppender.start();
        rootLogger.addAppender(customAppender);
    }
}

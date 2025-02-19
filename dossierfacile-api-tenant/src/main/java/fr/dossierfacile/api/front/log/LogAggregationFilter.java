package fr.dossierfacile.api.front.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Profile("!dev")
public class LogAggregationFilter extends OncePerRequestFilter {
    private LogstashTcpSocketAppender logstashAppender;

    @PostConstruct
    public void init() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logstashAppender = (LogstashTcpSocketAppender) rootLogger.getAppender("LOGSTASH");
        if (logstashAppender == null) {
            throw new IllegalStateException("Logstash appender (LOGSTASH) not found in Logback configuration.");
        }
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = MDC.get("request_id");
        try {
            filterChain.doFilter(request, response);
        } finally {
            List<LogModel> logs = CustomAppender.getLogsForRequest(requestId);
            String logMessage = logs.stream().map(LogModel::getMessage).collect(Collectors.joining());

            String enrichedLogs = String.format(
                    "Request completed: URI:%s, Method:%s, Status:%d, Logs:%s",
                    request.getRequestURI(),
                    request.getMethod(),
                    response.getStatus(),
                    logMessage
            );

            Level logLevel = logs.stream().map(LogModel::getLevel).max(Comparator.comparingInt(Level::toInt)).orElse(Level.INFO);

            LoggingEvent enrichedEvent = new LoggingEvent();
            enrichedEvent.setTimeStamp(System.currentTimeMillis());
            enrichedEvent.setLoggerName("AggregatedHttpLogger");
            enrichedEvent.setLevel(logLevel);
            enrichedEvent.setThreadName(Thread.currentThread().getName());
            enrichedEvent.setMessage(enrichedLogs);

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            enrichedEvent.setLoggerContext(loggerContext);

            logstashAppender.doAppend(enrichedEvent);

            CustomAppender.clearLogsForRequest(requestId);
        }
    }
}

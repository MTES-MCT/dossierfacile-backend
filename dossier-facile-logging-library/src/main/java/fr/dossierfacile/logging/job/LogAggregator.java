package fr.dossierfacile.logging.job;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.logging.appender.CustomAppender;
import fr.dossierfacile.logging.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "dossierfacile.logging.job.aggregator", havingValue = "true")
public class LogAggregator {

    private Logger rootLogger;
    private CustomAppender customAppender;

    final ObjectMapper objectMapper;

    public LogAggregator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void init() {
        rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        customAppender = CustomAppender.initCustomAppender(LoggerUtil.PROCESS_ID);
        rootLogger.addAppender(customAppender);
    }

    public void sendWorkerLogs(
            String processId,
            String actionType,
            Long startTime,
            Map<String, String> jobAttributes
    ) {
        LoggerUtil.prepareMDCForWorker(actionType, startTime, jobAttributes);
        var logs = customAppender.getLogsForUniqueIdentifier(processId);

        try {
            var logMessages = objectMapper.writeValueAsString(logs);
            LoggerUtil.addLogs(logMessages);
        } catch (JsonProcessingException e) {
            log.error("Error while sending worker logs", e);
        }

        String enrichedLogs = String.format(
                "Action %s on Queue %s completed with status %s in %s ms",
                LoggerUtil.getProcessAction(),
                LoggerUtil.getProcessQueueName(),
                LoggerUtil.getProcessJobStatus(),
                LoggerUtil.getExecutionTime()
        );

        Level logLevel = LoggerUtil.getLogLevel(logs);
        LoggerUtil.sendEnrichedLogs(rootLogger, logLevel, enrichedLogs);

        customAppender.clearLogsForRequest(processId);
        LoggerUtil.clearMDC();
    }

}

package fr.dossierfacile.logging.task;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.logging.appender.CustomAppender;
import fr.dossierfacile.logging.model.TaskStatus;
import fr.dossierfacile.logging.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@ConditionalOnProperty(name = "dossierfacile.logging.task.aggregator", havingValue = "true")
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

    public void sendLogs() {
        var processId = LoggerUtil.getProcessId();
        var logs = customAppender.getLogsForUniqueIdentifier(processId);
        try {
            var logMessages = objectMapper.writeValueAsString(logs);
            LoggerUtil.addLogs(logMessages);
        } catch (JsonProcessingException e) {
            log.warn("Error while adding aggregated logs", e);
            LoggerUtil.addLogs("Error while adding aggregated logs");
        }

        TaskStatus taskStatus = logs.stream().anyMatch(item -> item.getLevel() == Level.ERROR) ? TaskStatus.ERROR : TaskStatus.SUCCESS;
        LoggerUtil.addTaskStatus(taskStatus.name());

        var currentTime = System.currentTimeMillis();
        var executionTime = currentTime - Long.parseLong(LoggerUtil.getExecutionStartTime());

        LoggerUtil.addExecutionTime(executionTime);

        String enrichedLogs = String.format(
                "Task %s completed with status %s in %s ms",
                LoggerUtil.getTaskName(),
                taskStatus.name(),
                executionTime
        );

        Level logLevel = LoggerUtil.getLogLevel(logs);
        LoggerUtil.sendEnrichedLogs(rootLogger, logLevel, enrichedLogs);

        customAppender.clearLogsForRequest(processId);
        LoggerUtil.clearMDC();
    }
}

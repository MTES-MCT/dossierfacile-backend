package fr.dossierfacile.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.config.ApiVersion;
import fr.dossierfacile.common.entity.ApplicationLog;
import fr.dossierfacile.common.repository.ApplicationLogRepository;
import fr.dossierfacile.common.service.interfaces.ApplicationLogService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationLogServiceImpl implements ApplicationLogService {
    private final ApplicationLogRepository applicationLogRepository;
    private final ObjectMapper objectMapper;
    @Value("${application.api.version}")
    private Integer apiVersion;
    @Value("${application.name}")
    private String applicationName;

    @PostConstruct
    public void init() {
        var hasBeenUpgraded = applicationLogRepository.findFirstByLogTypeAndApiVersion(ApplicationLog.ApplicationLogType.UPGRADE, 4).isPresent();
        if (hasBeenUpgraded && !ApiVersion.V4.is(apiVersion)) {
            throw new UnsupportedOperationException("Application has been upgraded to version 4 downgrade cannot be performed");
        }
        if (!hasBeenUpgraded && ApiVersion.V4.is(apiVersion)) {
            ApplicationLog log = ApplicationLog.builder()
                    .logType(ApplicationLog.ApplicationLogType.UPGRADE)
                    .details(writeAsString(Map.of("version", 4)))
                    .build();
            save(log);
        }
    }

    @Override
    public void save(ApplicationLog log) {
        log.setCreationDateTime(LocalDateTime.now());
        log.setApplicationName(applicationName);
        log.setApiVersion(apiVersion);
        applicationLogRepository.save(log);
    }

    private String writeAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("FATAL: Cannot write log details as string", e);
        }
        return null;
    }
}

package fr.dossierfacile.api.pdf.service;

import fr.dossierfacile.api.pdf.service.interfaces.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CleanDocumentScheduledService {
    @Value("${file.retention.hours}")
    private long fileRetentionHours;
    @Autowired
    private DocumentService documentService;

    @Scheduled(cron = "${cron.process.cleanup}")
    public void scheduleTaskUsingCronExpression() {
        LocalDateTime date = LocalDateTime.now().minusHours(fileRetentionHours);
        log.info("Deleting watermark documents not downloaded and created before {}", date);
        documentService.cleanDocumentsBefore(date);
    }
}

package fr.dossierfacile.api.pdf.service;

import fr.dossierfacile.api.pdf.service.interfaces.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
public class CleanDocumentScheduledService {
    @Value("${file.retention.days}")
    private long fileRetentionDays;
    @Autowired
    private DocumentService documentService;

    @Scheduled(cron = "${cron.process.cleanup}")
    public void scheduleTaskUsingCronExpression() {
        log.info("Delete documents schedule tasks using cron jobs");
        LocalDateTime date = LocalDateTime.now().minusDays(fileRetentionDays).with(LocalTime.MIN);
        documentService.cleanDocumentsBefore(date);
    }
}

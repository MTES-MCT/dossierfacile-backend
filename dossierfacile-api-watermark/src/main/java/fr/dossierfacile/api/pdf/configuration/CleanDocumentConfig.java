package fr.dossierfacile.api.pdf.configuration;

import fr.dossierfacile.api.pdf.service.interfaces.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;


@Configuration
@EnableScheduling
@Slf4j
public class CleanDocumentConfig {

    @Autowired
    DocumentService documentService;

    @Scheduled(cron = "0 22 17 * * ?")
    public void scheduleTaskUsingCronExpression() {
        long now = System.currentTimeMillis() / 1000;
        log.info("Delete documents schedule tasks using cron jobs - " + now);
        documentService.cleanOldDocuments();
    }
}

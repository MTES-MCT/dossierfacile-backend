package fr.dossierfacile.api.pdfgenerator.amqp;


import fr.dossierfacile.api.pdfgenerator.log.LogAggregator;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.model.JobContext;
import fr.dossierfacile.common.model.JobStatus;
import fr.dossierfacile.common.utils.LoggerUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class ApartmentSharingConsumer {
    private final PdfGeneratorService pdfGeneratorService;
    private final LogAggregator logAggregator;

    @RabbitListener(queues = "${rabbitmq.queue.apartment-sharing.name}", containerFactory = "retryContainerFactory")
    public void receiveMessage(Map<String, String> item) throws IOException {
        var jobContext = new JobContext(null, null, QueueName.AMQP_APARTMENT_SHARING.name());
        LoggerUtil.addProcessId(jobContext.getProcessId());
        LoggerUtil.addApartmentSharing(item.get("id"));
        log.info("Received full PDF for apartment sharing to process:{}", item);
        try {
            pdfGeneratorService.generateFullDossierPdf(Long.valueOf(item.get("id")));
        } catch (Exception e) {
            jobContext.setJobStatus(JobStatus.ERROR);
            log.error(e.getMessage(), e.getCause());
            throw e;
        } finally {
            logAggregator.sendWorkerLogs(jobContext, ActionType.FULL_DOSSIER_PDF);
        }
    }

}

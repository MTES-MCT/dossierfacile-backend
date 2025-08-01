package fr.dossierfacile.api.pdfgenerator.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.api.pdfgenerator.amqp.model.DocumentModel;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.model.JobContext;
import fr.dossierfacile.common.model.JobStatus;
import fr.dossierfacile.common.utils.JobContextUtil;
import fr.dossierfacile.logging.job.LogAggregator;
import fr.dossierfacile.logging.util.LoggerUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class WatermarkPdfConsumer {
    private final Gson gson;
    private final PdfGeneratorService pdfGeneratorService;
    private final LogAggregator logAggregator;

    @RabbitListener(queues = "${rabbitmq.queue.watermark-generic.name}", containerFactory = "retryContainerFactory")
    public void receiveMessage(String message) {
        var jobContext = new JobContext(null, null, QueueName.AMQP_GENERIC_WATERMARK.name());
        LoggerUtil.addProcessId(jobContext.getProcessId());
        jobContext.setJobStatus(JobStatus.SUCCESS);
        log.info("Received message on watermark.generic API WM to process:{}", message);
        try {
            DocumentModel documentModel = gson.fromJson(message, DocumentModel.class);
            jobContext.setDocumentId(documentModel.getId());
            pdfGeneratorService.processPdfGenerationFormWatermark(documentModel.getId());
            jobContext.setJobStatus(JobStatus.SUCCESS);
        } catch (Exception e) {
            jobContext.setJobStatus(JobStatus.ERROR);
            log.error(e.getMessage(), e.getCause());
            throw e;
        } finally {
            try {
                logAggregator.sendWorkerLogs(
                        jobContext.getProcessId(),
                        ActionType.WATERMARK.name(),
                        jobContext.getStartTime(),
                        JobContextUtil.prepareJobAttributes(jobContext)
                );
            }
            catch (Exception e) {
                log.error("Error while sending logs for processId: {}", jobContext.getProcessId(), e);
            }
        }
    }

}

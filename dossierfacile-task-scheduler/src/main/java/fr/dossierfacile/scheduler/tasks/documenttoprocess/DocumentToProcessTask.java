package fr.dossierfacile.scheduler.tasks.documenttoprocess;

import com.google.gson.Gson;
import fr.dossierfacile.common.entity.Document;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentToProcessTask {

    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeFileProcess;
    @Value("${rabbitmq.routing.key.document.analyze}")
    private String routingKeyAnalyzeDocument;
    private final AmqpTemplate amqpTemplate;
    private final Gson gson;
    private final DocumentRepository documentRepository;

    @Scheduled(fixedDelayString = "${scheduled.process.document.to.process.analyse.delay}", timeUnit = TimeUnit.SECONDS)
    public void launchAnalysis() {
        log.debug("Start document analysis process");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromDateTime = now.minusMinutes(10);
        LocalDateTime toDateTime = now.minusMinutes(1);
        documentRepository.findToProcessWithoutAnalysisReportBetweenDate(fromDateTime, toDateTime)
                .forEach(this::sendForAnalysis);
    }

    private void sendForAnalysis(Document document) {
        Long documentId = document.getId();
        log.debug("Sending document with ID [{}] for analysis", documentId);
        amqpTemplate.send(exchangeFileProcess, routingKeyAnalyzeDocument, messageWithId(documentId));
    }

    private Message messageWithId(Long id) {
        Map<String, String> body = new TreeMap<>();
        body.putIfAbsent("id", String.valueOf(id));
        return new Message(gson.toJson(body).getBytes(), new MessageProperties());
    }

}

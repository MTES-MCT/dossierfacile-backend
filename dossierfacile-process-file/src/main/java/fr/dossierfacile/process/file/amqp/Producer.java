package fr.dossierfacile.process.file.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.process.file.amqp.model.TenantModel;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class Producer {
    private static final String EXCEPTION = "Sentry ID Exception: ";

    private final AmqpTemplate amqpTemplate;
    private final Gson gson;

    //File process
    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeFileProcess;
    @Value("${rabbitmq.routing.key.ocr}")
    private String routingKeyOcr;

    @Async
    public void processFile(Long tenantId) {
        try {
            TenantModel tenantModel = TenantModel.builder().id(tenantId).build();
            log.warn("Re-Sending tenantId [" + tenantId + "] to the end of the queue");
            amqpTemplate.convertAndSend(exchangeFileProcess, routingKeyOcr, gson.toJson(tenantModel));
        } catch (Exception e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error(e.getMessage(), e.getCause());
            throw e;
        }
    }
}

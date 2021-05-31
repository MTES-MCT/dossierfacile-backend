package fr.dossierfacile.api.front.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.api.front.amqp.model.TenantModel;
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
    private final AmqpTemplate amqpTemplate;
    private final Gson gson;

    @Value("${rabbitmq.exchange.file.process}")
    private String exchange;
    @Value("${rabbitmq.routing.key.ocr}")
    private String routingKeyOcr;

    @Async
    public void processFileOcr(Long id) {
        TenantModel tenantModel = TenantModel.builder().id(id).build();
        log.info("Send process file");
        amqpTemplate.convertAndSend(exchange, routingKeyOcr, gson.toJson(tenantModel));
    }
}

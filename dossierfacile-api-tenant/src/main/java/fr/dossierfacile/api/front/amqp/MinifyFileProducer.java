package fr.dossierfacile.api.front.amqp;


import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class MinifyFileProducer {
    private final AmqpTemplate amqpTemplate;
    private final Gson gson;

    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeFileProcess;
    @Value("${rabbitmq.routing.key.file.minify}")
    private String routingKeyMinify;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void minifyFile(Long fileId) {
        Map<String, String> body = new TreeMap<>();
        body.putIfAbsent("id", String.valueOf(fileId));
        Message msg  =  new Message( gson.toJson(body).getBytes(), new MessageProperties());

        log.info("Sending file with ID [" + fileId + "] for minifying");
        amqpTemplate.send(exchangeFileProcess, routingKeyMinify, msg);
    }

}

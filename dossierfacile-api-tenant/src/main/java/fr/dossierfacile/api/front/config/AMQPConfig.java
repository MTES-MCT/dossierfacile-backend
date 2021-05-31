package fr.dossierfacile.api.front.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AMQPConfig {

    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeFileProcess;

    @Value("${rabbitmq.queue.file.process.ocr}")
    private String queueFileProcessOcr;

    @Value("${rabbitmq.routing.key.ocr}")
    private String routingKeyOcr;

    @Bean
    Queue queueFileProcessOcr() {
        return new Queue(queueFileProcessOcr, true);
    }


    @Bean
    TopicExchange exchangeFileProcess() {
        return new TopicExchange(exchangeFileProcess);
    }


    @Bean
    Binding bindingQueueProcessFilesOcrExchangeFileProcess(Queue queueFileProcessOcr, TopicExchange exchangeFileProcess) {
        return BindingBuilder.bind(queueFileProcessOcr).to(exchangeFileProcess).with(routingKeyOcr);
    }

}

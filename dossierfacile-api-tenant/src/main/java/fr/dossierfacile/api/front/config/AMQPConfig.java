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

    //File process
    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeFileProcess;
    @Value("${rabbitmq.queue.file.process.ocr}")
    private String queueFileProcessOcr;
    @Value("${rabbitmq.routing.key.ocr}")
    private String routingKeyOcr;

    //Pdf generation
    @Value("${rabbitmq.exchange.pdf.generator}")
    private String exchangePdfGenerator;
    @Value("${rabbitmq.queue.pdf.generator}")
    private String queuePdfGenerator;
    @Value("${rabbitmq.routing.key.pdf.generator}")
    private String routingKeyPdfGenerator;

    @Bean
    Queue queueFileProcessOcr() {
        return new Queue(queueFileProcessOcr, true);
    }

    @Bean
    TopicExchange exchangeFileProcess() {
        return new TopicExchange(exchangeFileProcess);
    }

    @Bean
    Queue queuePdfGenerator() {
        return new Queue(queuePdfGenerator, true);
    }

    @Bean
    TopicExchange exchangePdfGenerator() {
        return new TopicExchange(exchangePdfGenerator);
    }

    @Bean
    Binding bindingQueueProcessFilesOcrExchangeFileProcess(Queue queueFileProcessOcr, TopicExchange exchangeFileProcess) {
        return BindingBuilder.bind(queueFileProcessOcr).to(exchangeFileProcess).with(routingKeyOcr);
    }

    @Bean
    Binding bindingQueuePdfGeneratorExchangePdfGenerator(Queue queuePdfGenerator, TopicExchange exchangePdfGenerator) {
        return BindingBuilder.bind(queuePdfGenerator).to(exchangePdfGenerator).with(routingKeyPdfGenerator);
    }

}

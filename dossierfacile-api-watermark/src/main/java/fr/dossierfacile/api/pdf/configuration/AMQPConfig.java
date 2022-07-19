package fr.dossierfacile.api.pdf.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AMQPConfig {
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Bean
    TopicExchange exchangePdfGenerator() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    Queue queuePdfGenerator() {
        return new Queue(queueName, true);
    }

    @Bean
    Binding bindingQueuePdfGeneratorExchangePdfGenerator(Queue queuePdfGenerator, TopicExchange exchangePdfGenerator) {
        return BindingBuilder.bind(queuePdfGenerator).to(exchangePdfGenerator).with(routingKey);
    }
}

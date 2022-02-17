package fr.dossierfacile.api.pdfgenerator.configuration;

import fr.dossierfacile.api.pdfgenerator.amqp.Receiver;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(
        prefix = "rabbitmq"
)
public class AMQPConfig {
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Value("${rabbitmq.prefetch}")
    private Integer prefetch;

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


    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setPrefetchCount(prefetch);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}

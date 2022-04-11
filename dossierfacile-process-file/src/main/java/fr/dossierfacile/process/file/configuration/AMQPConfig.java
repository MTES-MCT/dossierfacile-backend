package fr.dossierfacile.process.file.configuration;

import fr.dossierfacile.process.file.amqp.Receiver;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AMQPConfig {
    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeName;

    @Value("${rabbitmq.queue.file.process.ocr}")
    private String queueName;

    @Value("${rabbitmq.routing.key.ocr}")
    private String routingKey;

    @Value("${rabbitmq.prefetch}")
    private Integer prefetch;

    @Bean
    TopicExchange exchangeFileProcess() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    Queue queueFileProcessOcr() {
        return new Queue(queueName, true);
    }

    @Bean
    Binding bindingQueueProcessFilesOcrExchangeFileProcess(Queue queueFileProcessOcr, TopicExchange exchangeFileProcess) {
        return BindingBuilder.bind(queueFileProcessOcr).to(exchangeFileProcess).with(routingKey);
    }

    @Bean
    SimpleMessageListenerContainer containerOcr(ConnectionFactory connectionFactory,
                                                MessageListenerAdapter listenerAdapterOcr) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setPrefetchCount(prefetch);
        container.setMessageListener(listenerAdapterOcr);
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapterOcr(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}

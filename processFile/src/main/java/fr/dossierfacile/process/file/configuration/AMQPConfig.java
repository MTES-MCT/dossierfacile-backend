package fr.dossierfacile.process.file.configuration;

import fr.dossierfacile.process.file.amqp.ReceiverOcr;
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


    @Bean
    SimpleMessageListenerContainer containerOcr(ConnectionFactory connectionFactory,
                                                MessageListenerAdapter listenerAdapterOcr) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueFileProcessOcr);
        container.setMessageListener(listenerAdapterOcr);
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapterOcr(ReceiverOcr receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}

package fr.dossierfacile.api.pdfgenerator.configuration;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;

@Configuration
@ConfigurationProperties(
        prefix = "rabbitmq"
)
public class AMQPConfig {
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.watermark-document.name}")
    private String watermarkDocumentQueueName;

    @Value("${rabbitmq.queue.apartment-sharing.name}")
    private String apartmentSharingQueueName;

    @Value("${rabbitmq.queue.watermark-generic.name}")
    private String watermarkGenericQueueName;

    @Value("${rabbitmq.routing.key.apartment-sharing}")
    private String apartmentSharingRoutingKey;

    @Value("${rabbitmq.routing.key.watermark-generic}")
    private String watermarkGenericRoutingKey;

    @Value("${rabbitmq.routing.key.watermark-document}")
    private String watermarkDocumentRoutingKey;
    @Value("${rabbitmq.prefetch}")
    private Integer prefetch;

    @Bean
    TopicExchange exchangePdfGenerator() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    Queue queueWatermarkDocumentGenerator() {
        return new Queue(watermarkDocumentQueueName, true);
    }

    @Bean
    Queue queueApartmentSharingQueueName() {
        return new Queue(apartmentSharingQueueName, true);
    }
    @Bean
    Queue queueWatermarkGenerator() {
        return new Queue(watermarkGenericQueueName, true);
    }

    @Bean
    Binding bindingQueueWatermarkDocument(Queue queueWatermarkDocumentGenerator, TopicExchange exchangePdfGenerator) {
        return BindingBuilder.bind(queueWatermarkDocumentGenerator).to(exchangePdfGenerator).with(watermarkDocumentRoutingKey);
    }

    @Bean
    Binding bindingQueueApartmentSharing(Queue queueApartmentSharingQueueName, TopicExchange exchangePdfGenerator) {
        return BindingBuilder.bind(queueApartmentSharingQueueName).to(exchangePdfGenerator).with(apartmentSharingRoutingKey);
    }
    @Bean
    Binding bindingQueueWatermark(Queue queueWatermarkGenerator, TopicExchange exchangePdfGenerator) {
        return BindingBuilder.bind(queueWatermarkGenerator).to(exchangePdfGenerator).with(watermarkGenericRoutingKey);
    }

    // next step: use DLQ for unblocking retry instead of this blocking way - 3 retry - x5
    @Bean
    public SimpleRabbitListenerContainerFactory retryContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());

        Advice[] adviceChain = { RetryInterceptorBuilder.stateless()
                .backOffOptions(1000, 5.0, 15000)
                .maxAttempts(3)
                .recoverer( (r,t) -> new RejectAndDontRequeueRecoverer())
                .build() };
        factory.setAdviceChain(adviceChain);
        factory.setPrefetchCount(prefetch);

        return factory;
    }
}

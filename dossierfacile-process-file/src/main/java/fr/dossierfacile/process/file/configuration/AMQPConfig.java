package fr.dossierfacile.process.file.configuration;

import fr.dossierfacile.common.utils.Timeout;
import fr.dossierfacile.process.file.amqp.AnalyzeDocumentReceiver;
import fr.dossierfacile.process.file.amqp.AnalyzeFileReceiver;
import fr.dossierfacile.process.file.amqp.MinifyFileReceiver;
import fr.dossierfacile.process.file.service.AnalyzeDocumentService;
import fr.dossierfacile.process.file.service.AnalyzeFile;
import fr.dossierfacile.process.file.service.interfaces.MinifyFile;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;

import java.util.concurrent.TimeUnit;

@Profile("!noRabbit")
@Configuration
public class AMQPConfig {
    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeName;

    @Value("${rabbitmq.queue.file.analyze}")
    private String analyzeQueueName;

    @Value("${rabbitmq.queue.document.analyze}")
    private String analyzeDocumentQueueName;

    @Value("${rabbitmq.queue.file.minify}")
    private String minifyQueueName;

    @Value("${rabbitmq.routing.key.file.analyze}")
    private String analyzeRoutingKey;

    @Value("${rabbitmq.routing.key.document.analyze}")
    private String analyzeDocumentRoutingKey;

    @Value("${rabbitmq.routing.key.file.minify}")
    private String minifyRoutingKey;

    @Value("${rabbitmq.prefetch}")
    private Integer prefetch;

    @Bean
    TopicExchange exchangeFileProcess() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    Queue queueFileAnalyze() {
        return new Queue(analyzeQueueName, true);
    }

    @Bean
    Queue queueDocumentAnalyze() {
        return new Queue(analyzeDocumentQueueName, true);
    }

    @Bean
    Queue queueFileMinify() {
        return new Queue(minifyQueueName, true);
    }

    @Bean
    Binding bindingQueueAnalyzeFile(Queue queueFileAnalyze, TopicExchange exchangeFileProcess) {
        return BindingBuilder.bind(queueFileAnalyze).to(exchangeFileProcess).with(analyzeRoutingKey);
    }

    @Bean
    Binding bindingQueueAnalyzeDocument(Queue queueDocumentAnalyze, TopicExchange exchangeFileProcess) {
        return BindingBuilder.bind(queueDocumentAnalyze).to(exchangeFileProcess).with(analyzeDocumentRoutingKey);
    }
    @Bean
    Binding bindingQueueProcessMinifyFileProcess(Queue queueFileMinify, TopicExchange exchangeFileProcess) {
        return BindingBuilder.bind(queueFileMinify).to(exchangeFileProcess).with(minifyRoutingKey);
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

    @Bean
    AnalyzeFileReceiver analyzeFileReceiver(AnalyzeFile analyzeFile, @Value("${analysis.timeout.seconds}") int timeoutSeconds) {
        Timeout analysisTimeout = new Timeout(timeoutSeconds, TimeUnit.SECONDS);
        return new AnalyzeFileReceiver(analyzeFile, analysisTimeout);
    }

    @Bean
    MinifyFileReceiver minifyFileReceiver(MinifyFile minifyFile) {
        return new MinifyFileReceiver(minifyFile);
    }
    @Bean
    AnalyzeDocumentReceiver analyzeDocumentReceiver(AnalyzeDocumentService analyzeDocumentService) {
        return new AnalyzeDocumentReceiver(analyzeDocumentService);
    }

}

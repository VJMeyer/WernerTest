package de.kisters.hmt.cloud.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.file-upload}")
    private String queueName;

    @Value("${rabbitmq.exchange.file-processing}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key.file-upload}")
    private String routingKey;

    @Bean
    public Queue fileUploadQueue() {
        return new Queue(queueName, true); // durable queue
    }

    @Bean
    public TopicExchange fileProcessingExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Binding fileUploadBinding(Queue fileUploadQueue, TopicExchange fileProcessingExchange) {
        return BindingBuilder
                .bind(fileUploadQueue)
                .to(fileProcessingExchange)
                .with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}

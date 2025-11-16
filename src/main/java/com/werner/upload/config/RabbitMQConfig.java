package com.werner.upload.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String UPLOAD_EVENTS_EXCHANGE = "upload.events";
    public static final String UPLOAD_STATUS_QUEUE = "upload.status";
    public static final String UPLOAD_NOTIFICATION_QUEUE = "upload.notification";

    @Bean
    public TopicExchange uploadEventsExchange() {
        return ExchangeBuilder
                .topicExchange(UPLOAD_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue uploadStatusQueue() {
        return QueueBuilder
                .durable(UPLOAD_STATUS_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24 hours in milliseconds
                .build();
    }

    @Bean
    public Queue uploadNotificationQueue() {
        return QueueBuilder
                .durable(UPLOAD_NOTIFICATION_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24 hours in milliseconds
                .build();
    }

    @Bean
    public Binding uploadStatusBinding(Queue uploadStatusQueue, TopicExchange uploadEventsExchange) {
        return BindingBuilder
                .bind(uploadStatusQueue)
                .to(uploadEventsExchange)
                .with("upload.status.#");
    }

    @Bean
    public Binding uploadNotificationBinding(Queue uploadNotificationQueue, TopicExchange uploadEventsExchange) {
        return BindingBuilder
                .bind(uploadNotificationQueue)
                .to(uploadEventsExchange)
                .with("upload.notification.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }
}

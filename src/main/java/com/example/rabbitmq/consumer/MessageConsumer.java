package com.example.rabbitmq.consumer;

import com.example.rabbitmq.dto.MessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void consumeMessage(MessageDto message) {
        logger.info("Received message: {}", message);
        // Process the message here
        logger.info("Message processed: {}", message.getContent());
    }
}

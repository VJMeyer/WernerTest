package com.example.rabbitmq.controller;

import com.example.rabbitmq.dto.MessageDto;
import com.example.rabbitmq.producer.MessageProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageProducer messageProducer;

    public MessageController(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendMessage(@RequestBody Map<String, String> request) {
        String content = request.get("content");

        if (content == null || content.trim().isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Message content cannot be empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        MessageDto message = new MessageDto(content);
        messageProducer.sendMessage(message);

        Map<String, String> response = new HashMap<>();
        response.put("status", "Message sent successfully");
        response.put("content", content);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }
}

package com.example.rabbitmq.dto;

import java.time.LocalDateTime;

public class MessageDto {

    private String content;
    private LocalDateTime timestamp;

    public MessageDto() {
    }

    public MessageDto(String content) {
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MessageDto{" +
                "content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

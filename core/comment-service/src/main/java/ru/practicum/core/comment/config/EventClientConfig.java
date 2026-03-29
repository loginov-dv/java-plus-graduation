package ru.practicum.core.comment.config;

import org.springframework.context.annotation.Bean;

public class EventClientConfig {
    @Bean
    public EventErrorDecoder eventErrorDecoder() {
        return new EventErrorDecoder();
    }
}
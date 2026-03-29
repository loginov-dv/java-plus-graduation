package ru.practicum.core.event.config;

import org.springframework.context.annotation.Bean;

public class CommentClientConfig {
    @Bean
    public CommentErrorDecoder commentErrorDecoder() {
        return new CommentErrorDecoder();
    }
}
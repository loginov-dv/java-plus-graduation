package ru.practicum.core.comment.config;

import org.springframework.context.annotation.Bean;

public class UserClientConfig {
    @Bean
    public UserErrorDecoder userErrorDecoder() {
        return new UserErrorDecoder();
    }
}
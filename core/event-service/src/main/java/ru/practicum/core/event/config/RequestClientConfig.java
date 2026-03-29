package ru.practicum.core.event.config;

import org.springframework.context.annotation.Bean;

public class RequestClientConfig {
    @Bean
    public RequestErrorDecoder requestErrorDecoder() {
        return new RequestErrorDecoder();
    }
}
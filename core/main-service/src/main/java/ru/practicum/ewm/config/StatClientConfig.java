package ru.practicum.ewm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Lazy;
import ru.practicum.client.StatClient;
import ru.practicum.client.StatClientImpl;

@Configuration
public class StatClientConfig {

    @Bean
    @Lazy // иначе DiscoveryClient инициализируется преждевременно и main-service регистрируется в Eureka с портом 0
    public StatClient statClient(DiscoveryClient discoveryClient,
                                 @Value("${stats-server.id}") String statsServerId) {
        return new StatClientImpl(discoveryClient, statsServerId);
    }
}
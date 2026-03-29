package ru.practicum.core.request.service.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.EventApiContract;
import ru.practicum.core.request.config.EventClientConfig;

@FeignClient(
        name = "event-service",
        configuration = EventClientConfig.class)
public interface EventClient extends EventApiContract {
}
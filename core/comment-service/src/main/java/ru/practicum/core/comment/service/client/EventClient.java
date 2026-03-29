package ru.practicum.core.comment.service.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.comment.config.EventClientConfig;
import ru.practicum.core.common.api.EventApiContract;

@FeignClient(
        name = "event-service",
        configuration = EventClientConfig.class)
public interface EventClient extends EventApiContract {
}
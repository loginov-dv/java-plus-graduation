package ru.practicum.core.common.api.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.config.EventClientConfig;
import ru.practicum.core.common.api.contract.EventApiContract;
import ru.practicum.core.common.api.fallback.EventClientFallback;

@FeignClient(
        name = "event-service",
        configuration = EventClientConfig.class,
        fallback = EventClientFallback.class)
public interface EventClient extends EventApiContract {
}
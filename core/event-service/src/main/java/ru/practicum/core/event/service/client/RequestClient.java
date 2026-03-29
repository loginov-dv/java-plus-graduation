package ru.practicum.core.event.service.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.RequestApiContract;
import ru.practicum.core.event.config.RequestClientConfig;

@FeignClient(
        name = "request-service",
        configuration = RequestClientConfig.class)
public interface RequestClient extends RequestApiContract {
}
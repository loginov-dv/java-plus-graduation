package ru.practicum.core.common.api.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.contract.RequestApiContract;
import ru.practicum.core.common.api.config.RequestClientConfig;
import ru.practicum.core.common.api.fallback.RequestClientFallback;

@FeignClient(
        name = "request-service",
        configuration = RequestClientConfig.class,
        fallback = RequestClientFallback.class)
public interface RequestClient extends RequestApiContract {
}
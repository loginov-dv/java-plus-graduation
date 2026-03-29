package ru.practicum.core.request.service.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.UserApiContract;
import ru.practicum.core.request.config.UserClientConfig;

@FeignClient(
        name = "user-service",
        path = "/admin/users",
        configuration = UserClientConfig.class)
public interface UserClient extends UserApiContract {
}
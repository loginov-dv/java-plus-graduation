package ru.practicum.core.request.service.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.config.UserClientConfig;
import ru.practicum.core.common.api.contract.UserApiContract;

@FeignClient(
        name = "user-service",
        path = "/admin/users",
        configuration = UserClientConfig.class)
public interface UserClient extends UserApiContract {
}
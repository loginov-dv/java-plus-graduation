package ru.practicum.core.comment.service.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.comment.config.UserClientConfig;
import ru.practicum.core.common.api.UserApiContract;

@FeignClient(
        name = "user-service",
        path = "/admin/users",
        configuration = UserClientConfig.class)
public interface UserClient extends UserApiContract {
}
package ru.practicum.core.event.service.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.CommentApiContract;
import ru.practicum.core.event.config.CommentClientConfig;

@FeignClient(
        name = "comment-service",
        configuration = CommentClientConfig.class)
public interface CommentClient extends CommentApiContract {
}
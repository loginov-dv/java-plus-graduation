package ru.practicum.core.common.api.client;

import org.springframework.cloud.openfeign.FeignClient;

import ru.practicum.core.common.api.contract.CommentApiContract;
import ru.practicum.core.common.api.config.CommentClientConfig;
import ru.practicum.core.common.api.fallback.CommentClientFallback;

@FeignClient(
        name = "comment-service",
        configuration = CommentClientConfig.class,
        fallback = CommentClientFallback.class)
public interface CommentClient extends CommentApiContract {
}
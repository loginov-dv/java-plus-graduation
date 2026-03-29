package ru.practicum.core.common.api.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ru.practicum.core.common.api.contract.CommentApiContract;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CommentClientFallback implements CommentApiContract {
    @Override
    public Map<Long, Long> countByEvents(List<Long> eventIds) {
        log.warn("comment-service is not available, returning fallback response");

        return eventIds.stream()
                .collect(Collectors.toMap(Function.identity(), id -> 0L));
    }

    @Override
    public Long countByEvent(Long eventId) {
        log.warn("comment-service is not available, returning fallback response");

        return 0L;
    }
}
package ru.practicum.core.common.api.contract;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import ru.practicum.core.common.dto.event.EventFullDto;

// контракт для внутненнего API
public interface EventApiContract {
    @GetMapping("/events/{eventId}")
    EventFullDto getEvent(@PathVariable Long eventId,
                          HttpServletRequest request);

    @GetMapping("/events/inner/{eventId}")
    EventFullDto getEventInner(@PathVariable Long eventId);
}
package ru.practicum.core.common.api.fallback;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ru.practicum.core.common.api.contract.EventApiContract;
import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.dto.event.EventState;

@Slf4j
@Component
public class EventClientFallback implements EventApiContract {
    @Override
    public EventFullDto getEvent(Long eventId, HttpServletRequest request) {
        log.warn("event-service is not available, returning fallback response");

        return getDummyEventFullDto(eventId);
    }

    @Override
    public EventFullDto getEventInner(Long eventId) {
        log.warn("event-service is not available, returning fallback response");

        return getDummyEventFullDto(eventId);
    }

    private EventFullDto getDummyEventFullDto(Long eventId) {
        EventFullDto eventFullDto = new EventFullDto();

        eventFullDto.setId(eventId);
        eventFullDto.setConfirmedRequests(0L);
        eventFullDto.setDescription("unknown");
        eventFullDto.setState(EventState.PENDING.name());
        eventFullDto.setTitle("unknown");
        eventFullDto.setViews(0L);
        eventFullDto.setComments(0L);

        return eventFullDto;
    }
}
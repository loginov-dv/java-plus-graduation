package ru.practicum.core.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.practicum.client.StatsClient;
import ru.practicum.core.common.api.contract.EventApiContract;
import ru.practicum.core.common.dto.event.EventFullDto;
import ru.practicum.core.common.dto.event.EventPublicFilter;
import ru.practicum.core.common.dto.event.EventShortDto;
import ru.practicum.core.common.dto.page.PageRequestDto;
import ru.practicum.core.event.service.EventService;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.ewm.stats.proto.user.ActionTypeProto;
import ru.practicum.stats.client.CollectorClient;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/events")
@RestController
@Validated
public class PublicEventController implements EventApiContract {
    private static final String HEADER = "X-EWM-USER-ID";

    private final EventService eventService;
    /*@Lazy
    private final StatsClient statsClient;*/
    private final CollectorClient collectorClient;

    /*private void saveHit(HttpServletRequest request) {
        statsClient.hit(new EndpointHitDto(
                "event-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        ));
    }*/

    @GetMapping
    public List<EventShortDto> getEvents(@Valid EventPublicFilter eventPublicFilter,
                                         PageRequestDto pageRequestDto,
                                         HttpServletRequest request) {
        log.debug("GET /events");
        log.debug("EventPublicFilter: {}", eventPublicFilter);
        log.debug("PageRequestDto: {}", pageRequestDto);
        log.debug("Client ip: {}", request.getRemoteAddr());

        //saveHit(request);

        return eventService.publicSearchEvents(eventPublicFilter, pageRequestDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(@RequestHeader(HEADER) @Positive Long userId,
                                 @PathVariable Long eventId/*,
                                 HttpServletRequest request*/) {
        log.debug("GET /events/{}", eventId);
        //log.debug("Client ip: {}", request.getRemoteAddr());
        log.debug("User id: {}", userId);

        //saveHit(request);
        collectorClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
        log.debug("User action sent: user ({}) viewed event ({})", userId, eventId);

        return eventService.getPublishedEvent(eventId);
    }

    @GetMapping("recommendations")
    public List<EventShortDto> getRecommendations(@RequestHeader(HEADER) @Positive Long userId) {
        log.debug("GET /events/recommendations");
        log.debug("User id: {}", userId);

        return eventService.getRecommendations(userId);
    }

    @PutMapping("/{eventId}/like")
    public void putLike(@RequestHeader(HEADER) @Positive Long userId,
                        @PathVariable @Positive Long eventId) {
        log.debug("PUT /events/{}/like", eventId);
        log.debug("User id: {}", userId);

        eventService.putLike(userId, eventId);
    }

    // внутренний API без HttpServletRequest
    @Override
    @GetMapping("/inner/{eventId}")
    public EventFullDto getEventInner(@PathVariable Long eventId) {
        log.debug("GET /events/inner/{}", eventId);

        return eventService.getEvent(eventId);
    }
}
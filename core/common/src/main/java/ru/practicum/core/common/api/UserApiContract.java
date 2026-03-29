package ru.practicum.core.common.api;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.dto.user.UserShortDto;

import java.util.List;

// контракт для внутненнего API
public interface UserApiContract {
    @GetMapping("/{userId}")
    UserDto getById(@PathVariable @Positive Long userId);

    // TODO: fallback
    @GetMapping("/short/{userId}")
    UserShortDto getShortById(@PathVariable @Positive Long userId);

    // TODO: fallback
    @GetMapping("/short")
    List<UserShortDto> getShortByIdIn(@RequestParam List<Long> ids);
}
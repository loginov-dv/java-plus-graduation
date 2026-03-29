package ru.practicum.core.common.api.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ru.practicum.core.common.api.contract.UserApiContract;
import ru.practicum.core.common.dto.user.UserDto;
import ru.practicum.core.common.dto.user.UserShortDto;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UserClientFallback implements UserApiContract {
    @Override
    public UserDto getById(Long userId) {
        log.warn("user-service is not available, returning fallback response");

        UserDto userDto = new UserDto();

        userDto.setId(userId);
        userDto.setName("unknown");
        userDto.setEmail("unknown");

        return userDto;
    }

    @Override
    public UserShortDto getShortById(Long userId) {
        log.warn("user-service is not available, returning fallback response");

        return getDummyUserShortDto(userId);
    }

    @Override
    public List<UserShortDto> getShortByIdIn(List<Long> ids) {
        log.warn("user-service is not available, returning fallback response");

        List<UserShortDto> list = new ArrayList<>();

        for (Long id : ids) {
            list.add(getDummyUserShortDto(id));
        }

        return list;
    }

    private UserShortDto getDummyUserShortDto(Long userId) {
        UserShortDto userShortDto = new UserShortDto();

        userShortDto.setId(userId);
        userShortDto.setName("unknown");

        return userShortDto;
    }
}
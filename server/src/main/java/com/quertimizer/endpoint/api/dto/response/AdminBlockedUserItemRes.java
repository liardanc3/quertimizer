package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminBlockedUserItemRes {

    private final String userId;
    private final String ipAddress;
    private final LocalDateTime blockedAt;

}

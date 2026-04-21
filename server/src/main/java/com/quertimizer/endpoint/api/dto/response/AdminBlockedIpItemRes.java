package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminBlockedIpItemRes {

    private final String ipAddress;
    private final LocalDateTime blockedAt;

}

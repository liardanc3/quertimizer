package com.quertimizer.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserBlockedIp {

    private final String ipAddress;
    private final LocalDateTime blockedAt;
}

package com.quertimizer.user.domain.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserBlockedIp {

    private final String ipAddress;
    private final LocalDateTime blockedAt;
}

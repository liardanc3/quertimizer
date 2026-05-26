package com.quertimizer.auth.application.output;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthBlockedIpOutput {

    private final String ipAddress;
    private final LocalDateTime blockedAt;
}

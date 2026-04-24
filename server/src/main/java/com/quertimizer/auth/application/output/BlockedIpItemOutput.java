package com.quertimizer.auth.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BlockedIpItemOutput {

    private final String ipAddress;
    private final LocalDateTime blockedAt;
}

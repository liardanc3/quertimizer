package com.quertimizer.auth.application.output;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlockedUserItemOutput {

    private final String handle;
    private final String ipAddress;
    private final LocalDateTime blockedAt;
}

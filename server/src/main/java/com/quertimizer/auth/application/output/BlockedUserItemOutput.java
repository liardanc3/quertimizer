package com.quertimizer.auth.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BlockedUserItemOutput {

    private final String handle;
    private final String ipAddress;
    private final LocalDateTime blockedAt;
}

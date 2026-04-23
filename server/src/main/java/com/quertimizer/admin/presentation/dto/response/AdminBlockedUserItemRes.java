package com.quertimizer.admin.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminBlockedUserItemRes {

    private final String handle;
    private final String ipAddress;
    private final LocalDateTime blockedAt;

}

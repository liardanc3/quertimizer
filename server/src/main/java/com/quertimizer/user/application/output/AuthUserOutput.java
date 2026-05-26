package com.quertimizer.user.application.output;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.user.domain.model.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthUserOutput {

    private final String email;
    private final String handle;
    private final String password;
    private final UserRole role;
    private final DbmsType defaultDbms;
    private final String lastAccessIp;
    private final LocalDateTime lastAccessAt;
    private final boolean blocked;
    private final LocalDateTime blockedAt;
}

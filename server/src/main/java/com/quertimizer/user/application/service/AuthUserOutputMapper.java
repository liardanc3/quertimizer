package com.quertimizer.user.application.service;

import com.quertimizer.user.application.output.AuthUserOutput;
import com.quertimizer.user.domain.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthUserOutputMapper {

    public AuthUserOutput toOutput(User user) {
        return new AuthUserOutput(
                user.getEmail(), user.getHandle(), user.getPassword(), user.getResolvedRole(),
                user.getResolvedDefaultDbms(), user.getLastAccessIp(), user.getLastAccessAt(),
                user.isBlocked(), user.getBlockedAt()
        );
    }
}

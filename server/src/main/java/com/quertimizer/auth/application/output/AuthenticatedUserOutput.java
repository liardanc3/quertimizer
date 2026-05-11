package com.quertimizer.auth.application.output;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.auth.domain.model.AuthUser;
import lombok.Data;

@Data
public class AuthenticatedUserOutput {

    private final String email;
    private final String password;
    private final UserRole role;

    public static AuthenticatedUserOutput from(AuthUser user) {
        return new AuthenticatedUserOutput(user.getEmail(), user.getPassword(), user.getResolvedRole());
    }
}

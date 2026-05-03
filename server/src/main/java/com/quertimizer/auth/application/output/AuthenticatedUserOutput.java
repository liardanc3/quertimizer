package com.quertimizer.auth.application.output;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticatedUserOutput {

    private final String email;
    private final String password;
    private final UserRole role;

    public static AuthenticatedUserOutput from(User user) {
        return new AuthenticatedUserOutput(user.getEmail(), user.getPassword(), user.getResolvedRole());
    }
}

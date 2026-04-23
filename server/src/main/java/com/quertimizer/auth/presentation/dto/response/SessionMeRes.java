package com.quertimizer.auth.presentation.dto.response;

import com.quertimizer.auth.application.result.AuthenticatedSessionResult;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionMeRes {

    private final boolean authenticated;
    private final String handle;
    private final String defaultDbms;
    private final String role;
    private final boolean handleSetupRequired;

    public static SessionMeRes from(AuthenticatedSessionResult result) {
        if (!result.authenticated()) {
            return unauthenticated();
        }

        return authenticated(
                result.handle(),
                result.handleSetupRequired(),
                result.defaultDbms(),
                result.role()
        );
    }

    public static SessionMeRes authenticated(String handle,
                                             boolean handleSetupRequired,
                                             DbmsType defaultDbms,
                                             String role) {
        return new SessionMeRes(
                true,
                handle,
                defaultDbms != null ? defaultDbms.getValue() : null,
                role,
                handleSetupRequired
        );
    }

    public static SessionMeRes authenticated(String handle,
                                             boolean handleSetupRequired,
                                             DbmsType defaultDbms,
                                             UserRole role) {
        return authenticated(
                handle,
                handleSetupRequired,
                defaultDbms,
                role != null ? resolveRole(role) : null
        );
    }

    public static SessionMeRes unauthenticated() {
        return new SessionMeRes(false, null, null, null, false);
    }

    private static String resolveRole(UserRole role) {
        return switch (role) {
            case USER -> "user";
            case ADMIN -> "admin";
            case PROBLEM_GENERATOR -> "problem_generator";
        };
    }
}

package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.constant.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionMeRes {

    private final boolean authenticated;
    private final String userId;
    private final String defaultDbms;
    private final String role;
    private final boolean userIdSetupRequired;

    public static SessionMeRes authenticated(String userId,
                                             boolean userIdSetupRequired,
                                             DbmsType defaultDbms,
                                             String role) {
        return new SessionMeRes(
                true,
                userId,
                defaultDbms != null ? defaultDbms.getValue() : null,
                role,
                userIdSetupRequired
        );
    }

    public static SessionMeRes authenticated(String userId,
                                             boolean userIdSetupRequired,
                                             DbmsType defaultDbms,
                                             UserRole role) {
        return authenticated(
                userId,
                userIdSetupRequired,
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

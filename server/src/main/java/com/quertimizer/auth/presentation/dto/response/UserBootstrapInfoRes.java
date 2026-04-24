package com.quertimizer.auth.presentation.dto.response;

import com.quertimizer.auth.application.output.UserBootstrapOutput;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserBootstrapInfoRes {

    private final boolean authenticated;
    private final String handle;
    private final String defaultDbms;
    private final String role;
    private final boolean handleSetupRequired;

    public static UserBootstrapInfoRes from(UserBootstrapOutput result) {
        if (!result.isAuthenticated()) {
            return unauthenticated();
        }

        return authenticated(result.getHandle(), result.isHandleSetupRequired(), result.getDefaultDbms(), result.getRole());
    }

    public static UserBootstrapInfoRes authenticated(String handle, boolean handleSetupRequired, DbmsType defaultDbms, String role) {
        return new UserBootstrapInfoRes(true, handle, defaultDbms != null ? defaultDbms.getValue() : null, role, handleSetupRequired);
    }

    public static UserBootstrapInfoRes authenticated(String handle, boolean handleSetupRequired, DbmsType defaultDbms, UserRole role) {
        return authenticated(handle, handleSetupRequired, defaultDbms, role != null ? resolveRole(role) : null);
    }

    public static UserBootstrapInfoRes unauthenticated() {
        return new UserBootstrapInfoRes(false, null, null, null, false);
    }

    private static String resolveRole(UserRole role) {
        return switch (role) {
            case USER -> "user";
            case ADMIN -> "admin";
            case PROBLEM_GENERATOR -> "problem_generator";
        };
    }
}

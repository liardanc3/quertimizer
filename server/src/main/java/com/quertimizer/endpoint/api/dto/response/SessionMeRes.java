package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.constant.DbmsType;
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

    public static SessionMeRes authenticated(String userId, DbmsType defaultDbms, String role) {
        return new SessionMeRes(
                true,
                userId,
                defaultDbms != null ? defaultDbms.getValue() : null,
                role
        );
    }

    public static SessionMeRes unauthenticated() {
        return new SessionMeRes(false, null, null, null);
    }
}

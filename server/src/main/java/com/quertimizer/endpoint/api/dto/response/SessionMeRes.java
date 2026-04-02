package com.quertimizer.endpoint.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionMeRes {

    private final boolean authenticated;
    private final String userId;

    public static SessionMeRes authenticated(String userId) {
        return new SessionMeRes(true, userId);
    }

    public static SessionMeRes unauthenticated() {
        return new SessionMeRes(false, null);
    }
}

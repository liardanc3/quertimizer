package com.quertimizer.auth.adapter.in.web.response;

import com.quertimizer.auth.application.output.AuthManageOutput;
import lombok.Getter;

import java.util.List;

@Getter
public class AuthManageRes {

    private final List<AuthManageUserRowRes> users;

    public AuthManageRes(List<AuthManageUserRowRes> users) {
        this.users = users;
    }

    public static AuthManageRes from(AuthManageOutput result) {
        return new AuthManageRes(result.getUsers().stream()
                .map(AuthManageUserRowRes::from)
                .toList());
    }
}

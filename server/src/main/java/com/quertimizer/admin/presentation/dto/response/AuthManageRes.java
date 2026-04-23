package com.quertimizer.admin.presentation.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class AuthManageRes {

    private final List<AuthManageUserRowRes> users;

    public AuthManageRes(List<AuthManageUserRowRes> users) {
        this.users = users;
    }

}

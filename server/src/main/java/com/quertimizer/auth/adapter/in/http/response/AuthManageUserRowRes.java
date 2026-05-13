package com.quertimizer.auth.adapter.in.http.response;

import com.quertimizer.auth.application.output.AuthManageUserRowOutput;
import lombok.Data;

@Data
public class AuthManageUserRowRes {

    private final String handle;
    private final String role;

    public static AuthManageUserRowRes from(AuthManageUserRowOutput result) {
        return new AuthManageUserRowRes(result.getHandle(), result.getRole());
    }
}

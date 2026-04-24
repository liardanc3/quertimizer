package com.quertimizer.auth.presentation.dto.response;

import com.quertimizer.auth.application.output.AuthManageUserRowOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthManageUserRowRes {

    private final String handle;
    private final String role;
    private final List<String> permissionKeys;

    public static AuthManageUserRowRes from(AuthManageUserRowOutput result) {
        return new AuthManageUserRowRes(
                result.getHandle(),
                result.getRole(),
                result.getPermissionKeys()
        );
    }
}

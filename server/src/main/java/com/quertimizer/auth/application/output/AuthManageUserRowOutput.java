package com.quertimizer.auth.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthManageUserRowOutput {

    private final String handle;
    private final String role;
    private final List<String> permissionKeys;
}

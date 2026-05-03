package com.quertimizer.auth.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthManageUserRowOutput {

    private final String handle;
    private final String role;
}

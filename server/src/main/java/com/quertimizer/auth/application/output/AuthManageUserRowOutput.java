package com.quertimizer.auth.application.output;

import lombok.Data;

@Data
public class AuthManageUserRowOutput {

    private final String handle;
    private final String role;
}

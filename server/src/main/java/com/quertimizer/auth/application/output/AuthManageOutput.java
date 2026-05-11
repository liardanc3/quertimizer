package com.quertimizer.auth.application.output;

import lombok.Data;

import java.util.List;

@Data
public class AuthManageOutput {

    private final List<AuthManageUserRowOutput> users;
}

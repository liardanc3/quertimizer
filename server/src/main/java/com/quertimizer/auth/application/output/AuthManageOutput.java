package com.quertimizer.auth.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthManageOutput {

    private final List<AuthManageUserRowOutput> users;
}

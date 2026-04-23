package com.quertimizer.admin.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthManageUserRowRes {

    private final String handle;
    private final String role;
    private final List<String> permissionKeys;

}

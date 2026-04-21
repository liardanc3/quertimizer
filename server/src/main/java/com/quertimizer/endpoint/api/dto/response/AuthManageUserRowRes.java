package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthManageUserRowRes {

    private final String userId;
    private final String role;
    private final List<String> permissionKeys;

}

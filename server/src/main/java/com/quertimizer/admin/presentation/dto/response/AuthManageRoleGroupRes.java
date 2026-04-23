package com.quertimizer.admin.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthManageRoleGroupRes {

    private final int count;
    private final List<AuthManageMemberRes> members;

}

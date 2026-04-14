package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthManageRes {

    private final AuthManageRoleGroupRes admins;
    private final AuthManageRoleGroupRes users;
    private final AuthManageProblemGeneratorGroupRes problemGenerators;

}

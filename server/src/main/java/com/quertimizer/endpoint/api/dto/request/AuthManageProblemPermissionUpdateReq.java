package com.quertimizer.endpoint.api.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AuthManageProblemPermissionUpdateReq {

    private List<String> permissionKeys;

}

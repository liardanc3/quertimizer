package com.quertimizer.auth.presentation.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AuthManageProblemPermissionUpdateReq {

    @Size(max = 200)
    private List<@Size(max = 40) String> permissionKeys;

}

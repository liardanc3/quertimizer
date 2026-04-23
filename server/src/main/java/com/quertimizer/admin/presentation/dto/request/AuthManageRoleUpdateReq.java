package com.quertimizer.admin.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuthManageRoleUpdateReq {

    @NotBlank
    @Size(max = 30)
    private String role;

}

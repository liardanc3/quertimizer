package com.quertimizer.auth.adapter.in.web.request;

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

    @NotBlank
    @Size(max = 64)
    private String confirmationText;
}

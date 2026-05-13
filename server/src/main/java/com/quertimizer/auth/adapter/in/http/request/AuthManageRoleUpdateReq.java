package com.quertimizer.auth.adapter.in.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthManageRoleUpdateReq {

    @NotBlank
    @Size(max = 30)
    private String role;

    @NotBlank
    @Size(max = 64)
    private String confirmationText;
}

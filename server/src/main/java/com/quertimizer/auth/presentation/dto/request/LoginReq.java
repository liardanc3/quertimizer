package com.quertimizer.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class LoginReq {

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Pattern(
            regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
            message = "올바른 이메일 형식으로 입력해 주세요."
    )
    private String email;

    @Pattern(regexp = "^[A-Fa-f0-9]{128}$")
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;
    private boolean rememberLogin;
}

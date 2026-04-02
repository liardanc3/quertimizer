package com.quertimizer.endpoint.api.dto.request;

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

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{1,15}$",
            message = "아이디는 영문, 숫자, '_', '-' 만 사용할 수 있고 최대 15자까지 입력할 수 있습니다."
    )
    private String userId;

    @Pattern(regexp = "^[A-Fa-f0-9]{128}$")
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;
    private boolean rememberLogin;
}

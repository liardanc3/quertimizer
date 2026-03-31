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
public class SignupReq {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{1,20}$",
            message = "아이디는 영문, 숫자, '_', '-' 만 사용하며 최대 20자까지 입력할 수 있습니다."
    )
    private String userId;

    // 앞단에서 SHA512(비밀번호) 전달받음
    @Pattern(regexp = "^[A-Fa-f0-9]{128}$")
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Pattern(
            regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
            message = "올바른 이메일 형식으로 입력해 주세요."
    )
    private String email;
}

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
public class ResetPasswordReq {

    @NotBlank(message = "이메일을 입력해 주세요")
    @Pattern(
            regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
            message = "올바른 이메일 형식으로 입력해 주세요"
    )
    private String email;

    @NotBlank(message = "인증코드를 입력해 주세요")
    @Pattern(
            regexp = "^[A-Z0-9]{6}$",
            message = "인증코드 6자를 정확히 입력해 주세요"
    )
    private String code;

    @Pattern(regexp = "^[A-Fa-f0-9]{128}$")
    @NotBlank(message = "비밀번호를 입력해 주세요")
    private String password;
}

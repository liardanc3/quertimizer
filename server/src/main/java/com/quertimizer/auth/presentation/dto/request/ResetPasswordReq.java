package com.quertimizer.auth.presentation.dto.request;

import com.quertimizer.auth.application.input.ResetPasswordInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.quertimizer.auth.domain.model.AuthValidationMessage.CODE_FORMAT_INVALID;
import static com.quertimizer.auth.domain.model.AuthValidationMessage.CODE_REQUIRED;
import static com.quertimizer.auth.domain.model.AuthValidationMessage.EMAIL_FORMAT_INVALID;
import static com.quertimizer.auth.domain.model.AuthValidationMessage.EMAIL_REQUIRED;
import static com.quertimizer.auth.domain.model.AuthValidationMessage.PASSWORD_REQUIRED;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ResetPasswordReq {

    @NotBlank(message = EMAIL_REQUIRED)
    @Pattern(
            regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
            message = EMAIL_FORMAT_INVALID
    )
    private String email;

    @NotBlank(message = CODE_REQUIRED)
    @Pattern(
            regexp = "^[A-Z0-9]{6}$",
            message = CODE_FORMAT_INVALID
    )
    private String code;

    @Pattern(regexp = "^[A-Fa-f0-9]{128}$")
    @NotBlank(message = PASSWORD_REQUIRED)
    private String password;

    public ResetPasswordInput toResetPasswordInput() {
        return new ResetPasswordInput(email, code, password);
    }
}

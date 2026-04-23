package com.quertimizer.auth.presentation.dto.request;

import com.quertimizer.auth.application.input.AccountRecoveryEmailInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.quertimizer.auth.domain.model.AuthValidationMessage.EMAIL_FORMAT_INVALID;
import static com.quertimizer.auth.domain.model.AuthValidationMessage.EMAIL_REQUIRED;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AccountRecoveryEmailReq {

    @NotBlank(message = EMAIL_REQUIRED)
    @Pattern(
            regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
            message = EMAIL_FORMAT_INVALID
    )
    private String email;

    public AccountRecoveryEmailInput toAccountRecoveryEmailInput() {
        return new AccountRecoveryEmailInput(email);
    }
}

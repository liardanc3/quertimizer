package com.quertimizer.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.quertimizer.auth.domain.model.AuthValidationMessage.EMAIL_FORMAT_INVALID_WITH_PERIOD;
import static com.quertimizer.auth.domain.model.AuthValidationMessage.EMAIL_REQUIRED;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class DuplicateCheckEmailReq {

    @NotBlank(message = EMAIL_REQUIRED)
    @Pattern(
            regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
            message = EMAIL_FORMAT_INVALID_WITH_PERIOD
    )
    private String email;
}

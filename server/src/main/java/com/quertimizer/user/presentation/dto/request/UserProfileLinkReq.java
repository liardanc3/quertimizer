package com.quertimizer.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.quertimizer.user.domain.model.UserValidationMessage.LINK_TYPE_LENGTH_EXCEEDED;
import static com.quertimizer.user.domain.model.UserValidationMessage.LINK_TYPE_PIPE_UNAVAILABLE;
import static com.quertimizer.user.domain.model.UserValidationMessage.LINK_TYPE_REQUIRED;
import static com.quertimizer.user.domain.model.UserValidationMessage.LINK_VALUE_LENGTH_EXCEEDED;
import static com.quertimizer.user.domain.model.UserValidationMessage.LINK_VALUE_PIPE_UNAVAILABLE;
import static com.quertimizer.user.domain.model.UserValidationMessage.LINK_VALUE_REQUIRED;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserProfileLinkReq {

    @NotBlank(message = LINK_TYPE_REQUIRED)
    @Size(max = 30, message = LINK_TYPE_LENGTH_EXCEEDED)
    @Pattern(regexp = "^[^|]+$", message = LINK_TYPE_PIPE_UNAVAILABLE)
    private String type;

    @NotBlank(message = LINK_VALUE_REQUIRED)
    @Size(max = 255, message = LINK_VALUE_LENGTH_EXCEEDED)
    @Pattern(regexp = "^[^|]+$", message = LINK_VALUE_PIPE_UNAVAILABLE)
    private String value;

}

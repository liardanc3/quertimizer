package com.quertimizer.user.adapter.in.http.request;

import com.quertimizer.user.application.input.UserProfileLinkInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserProfileLinkReq {

    @NotBlank(message = "링크 타입을 입력해 주세요.")
    @Size(max = 30, message = "링크 타입은 최대 30자까지 입력할 수 있습니다.")
    @Pattern(regexp = "^[^|]+$", message = "링크 타입에는 | 문자를 사용할 수 없습니다.")
    private String type;

    @NotBlank(message = "링크 값을 입력해 주세요.")
    @Size(max = 255, message = "링크 값은 최대 255자까지 입력할 수 있습니다.")
    @Pattern(regexp = "^[^|]+$", message = "링크 값에는 | 문자를 사용할 수 없습니다.")
    private String value;

    public UserProfileLinkInput toUserProfileLinkInput() {
        return new UserProfileLinkInput(type, value);
    }
}

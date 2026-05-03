package com.quertimizer.auth.adapter.in.web.request;

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
public class SetupHandleReq {

    @NotBlank(message = "Handle을 입력해 주세요.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{1,15}$",
            message = "영문, 숫자, 언더스코어(_)와 하이픈(-)만 사용할 수 있으며 최대 15자까지 입력할 수 있습니다."
    )
    private String handle;
}

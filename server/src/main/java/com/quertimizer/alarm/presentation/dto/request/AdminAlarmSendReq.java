package com.quertimizer.alarm.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AdminAlarmSendReq {

    @NotEmpty
    private List<@NotBlank @Size(max = 50) String> recipientHandles;

    @NotBlank
    @Size(max = 500)
    private String message;

}

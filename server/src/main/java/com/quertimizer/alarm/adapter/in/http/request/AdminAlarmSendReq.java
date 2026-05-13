package com.quertimizer.alarm.adapter.in.http.request;

import com.quertimizer.alarm.application.input.SendAdminAlarmInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AdminAlarmSendReq {

    @NotEmpty
    private List<@NotBlank @Size(max = 50) String> recipientHandles;

    @NotBlank
    @Size(max = 500)
    private String message;

    public SendAdminAlarmInput toSendAdminAlarmInput() {
        return new SendAdminAlarmInput(recipientHandles, message);
    }

}

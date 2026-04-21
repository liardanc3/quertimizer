package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.request.AdminAlarmSendReq;
import com.quertimizer.endpoint.api.dto.response.AdminAlarmRecipientRes;
import com.quertimizer.endpoint.api.dto.response.AdminAlarmSendRes;
import com.quertimizer.service.AlarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminAlarmController {

    private final AlarmService alarmService;

    @GetMapping("/admin/alarms/recipients")
    public ResponseEntity<List<AdminAlarmRecipientRes>> searchAlarmRecipients(@RequestParam String keyword) {

        return ResponseEntity.ok(alarmService.searchRecipientHandles(keyword).stream()
                .map(AdminAlarmRecipientRes::new)
                .toList());
    }

    @PostMapping("/admin/alarms/send")
    public ResponseEntity<AdminAlarmSendRes> sendAdminAlarm(@Valid @RequestBody AdminAlarmSendReq request) {

        return ResponseEntity.ok(new AdminAlarmSendRes(
                alarmService.sendAdminAlarm(request.getRecipientHandles(), request.getMessage())
        ));
    }

}

package com.quertimizer.alarm.presentation.controller;

import com.quertimizer.alarm.presentation.dto.request.AdminAlarmSendReq;
import com.quertimizer.alarm.presentation.dto.response.AdminAlarmRecipientRes;
import com.quertimizer.alarm.presentation.dto.response.AdminAlarmSendRes;
import com.quertimizer.alarm.application.usecase.SearchAlarmRecipientHandles;
import com.quertimizer.alarm.application.usecase.SendAdminAlarm;
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

    private final SearchAlarmRecipientHandles searchAlarmRecipientHandles;
    private final SendAdminAlarm sendAdminAlarm;

    @GetMapping("/admin/alarms/recipients")
    public ResponseEntity<List<AdminAlarmRecipientRes>> searchAlarmRecipients(@RequestParam String keyword) {
        // 관리자 알람 수신 Handle 후보를 조회
        return ResponseEntity.ok(searchAlarmRecipientHandles.execute(keyword).stream()
                .map(AdminAlarmRecipientRes::new)
                .toList());
    }

    @PostMapping("/admin/alarms/send")
    public ResponseEntity<AdminAlarmSendRes> sendAdminAlarm(@Valid @RequestBody AdminAlarmSendReq request) {
        // 관리자 공지 알람을 전송
        return ResponseEntity.ok(new AdminAlarmSendRes(
                sendAdminAlarm.execute(request.getRecipientHandles(), request.getMessage())
        ));
    }
}

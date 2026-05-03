package com.quertimizer.alarm.adapter.in.web;

import com.quertimizer.alarm.application.port.in.SearchAlarmRecipientHandlesUseCase;
import com.quertimizer.alarm.application.port.in.SendAdminAlarmUseCase;
import com.quertimizer.alarm.adapter.in.web.request.AdminAlarmSendReq;
import com.quertimizer.alarm.adapter.in.web.response.AdminAlarmRecipientRes;
import com.quertimizer.alarm.adapter.in.web.response.AdminAlarmSendRes;
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

    private final SearchAlarmRecipientHandlesUseCase searchAlarmRecipientHandles;
    private final SendAdminAlarmUseCase sendAdminAlarm;

    /**
     * 관리자 알람 수신 handle 후보를 검색한다.
     *
     * @param keyword 수신자 검색어
     */
    @GetMapping("/admin/alarms/recipients")
    public ResponseEntity<List<AdminAlarmRecipientRes>> searchAlarmRecipients(@RequestParam String keyword) {
        return ResponseEntity.ok(searchAlarmRecipientHandles.execute(keyword).stream()
                .map(AdminAlarmRecipientRes::new)
                .toList());
    }

    /**
     * 관리자가 지정한 사용자들에게 공지 알람을 전송한다.
     *
     * @param request 수신 handle 목록과 메시지 요청
     */
    @PostMapping("/admin/alarms/send")
    public ResponseEntity<AdminAlarmSendRes> sendAdminAlarm(@Valid @RequestBody AdminAlarmSendReq request) {
        return ResponseEntity.ok(new AdminAlarmSendRes(sendAdminAlarm.execute(request.toSendAdminAlarmInput())));
    }
}

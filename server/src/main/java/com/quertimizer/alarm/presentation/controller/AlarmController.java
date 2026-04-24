package com.quertimizer.alarm.presentation.controller;

import com.quertimizer.alarm.presentation.dto.response.AlarmPageRes;
import com.quertimizer.alarm.application.usecase.GetAlarms;
import com.quertimizer.alarm.application.usecase.MarkAlarmRead;
import com.quertimizer.alarm.application.usecase.MarkAllAlarmsRead;
import com.quertimizer.alarm.presentation.support.AlarmSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AlarmController {

    private final GetAlarms getAlarms;
    private final MarkAllAlarmsRead markAllAlarmsRead;
    private final MarkAlarmRead markAlarmRead;

    private final AlarmSupport alarmSupport;

    @GetMapping("/alarms")
    public ResponseEntity<AlarmPageRes> getAlarms(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(required = false) Integer pageSize,
                                                  Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = alarmSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 현재 사용자 알람 목록을 조회
        return ResponseEntity.ok(AlarmPageRes.from(getAlarms.execute(currentHandle, page, pageSize)));
    }

    @PostMapping("/alarms/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = alarmSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 현재 사용자 알람을 모두 읽음 처리
        markAllAlarmsRead.execute(currentHandle);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alarms/{alarmId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long alarmId,
                                         Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = alarmSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 현재 사용자 알람을 개별 읽음 처리
        return markAlarmRead.execute(alarmId, currentHandle)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

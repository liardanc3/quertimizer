package com.quertimizer.alarm.presentation.controller;

import com.quertimizer.alarm.application.input.AlarmPageInput;
import com.quertimizer.alarm.application.input.MarkAlarmReadInput;
import com.quertimizer.alarm.application.usecase.GetAlarms;
import com.quertimizer.alarm.application.usecase.MarkAlarmRead;
import com.quertimizer.alarm.application.usecase.MarkAllAlarmsRead;
import com.quertimizer.alarm.presentation.dto.response.AlarmPageRes;
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

    /**
     * 현재 사용자의 알람 페이지를 반환한다.
     *
     * <ol>
     *   <li>인증 handle 확인
     *   <li>알람 페이지 조회 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/alarms")
    public ResponseEntity<AlarmPageRes> getAlarms(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(required = false) Integer pageSize,
                                                  @RequestParam(defaultValue = "desc") String createdAtSort,
                                                  Authentication authentication) {
        String currentHandle = alarmSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AlarmPageInput input = new AlarmPageInput(currentHandle, page, pageSize, createdAtSort);
        return ResponseEntity.ok(AlarmPageRes.from(getAlarms.execute(input)));
    }

    /**
     * 현재 사용자의 모든 알람을 읽음 처리한다.
     *
     * <ol>
     *   <li>인증 handle 확인
     *   <li>모든 알람 읽음 처리
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @PostMapping("/alarms/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        String currentHandle = alarmSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        markAllAlarmsRead.execute(currentHandle);
        return ResponseEntity.noContent().build();
    }

    /**
     * 현재 사용자의 단일 알람을 읽음 처리한다.
     *
     * <ol>
     *   <li>인증 handle 확인
     *   <li>단일 알람 읽음 처리 응답 생성
     * </ol>
     *
     * @param alarmId 읽음 처리할 알람 ID
     * @param authentication 현재 요청의 인증 정보
     */
    @PostMapping("/alarms/{alarmId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long alarmId,
                                         Authentication authentication) {
        String currentHandle = alarmSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        MarkAlarmReadInput input = new MarkAlarmReadInput(alarmId, currentHandle);
        return markAlarmRead.execute(input)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchAlarmRecipientHandles {

    private final AlarmService alarmService;

    public List<String> execute(String keyword) {
        // 관리자 알람 수신 Handle 후보를 검색
        return alarmService.searchRecipientHandles(keyword);
    }
}

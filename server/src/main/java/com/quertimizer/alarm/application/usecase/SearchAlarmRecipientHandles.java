package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchAlarmRecipientHandles {

    private final AlarmService alarmService;

    /**
     * 관리자 알람 수신 handle 후보를 검색한다.
     *
     * @param keyword 수신자 검색어
     */
    public List<String> execute(String keyword) {
        return alarmService.searchRecipientHandles(keyword);
    }
}

package com.quertimizer.alarm.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.alarm.application.port.in.SearchAlarmRecipientHandlesUseCase;
import com.quertimizer.alarm.application.port.out.AlarmRecipientPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchAlarmRecipientHandles implements SearchAlarmRecipientHandlesUseCase {

    private final AlarmRecipientPort alarmRecipientPort;

    /**
     * 관리자 알람 수신 handle 후보를 검색한다.
     *
     * <ol>
     *   <li>검색어 정규화
     *   <li>검색어 기준 사용자 handle 후보 조회
     * </ol>
     *
     * @param keyword 수신자 검색어
     */
    @Transactional(readOnly = true)
    @Override
    @Log("알람 수신자 검색")
    public List<String> execute(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isBlank()) {
            return List.of();
        }

        return alarmRecipientPort.searchHandles(normalizedKeyword);
    }
}

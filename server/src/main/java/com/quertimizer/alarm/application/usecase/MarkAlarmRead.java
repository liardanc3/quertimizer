package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.input.MarkAlarmReadInput;
import com.quertimizer.alarm.application.port.UserAlarmRepository;
import com.quertimizer.alarm.domain.entity.UserAlarm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MarkAlarmRead {

    private final UserAlarmRepository userAlarmRepository;

    /**
     * 사용자 단일 알람을 읽음 처리한다.
     *
     * <ol>
     *   <li>알람 소유자 기준 알람 조회
     *   <li>미확인 알람 읽음 처리
     * </ol>
     *
     * @param input 읽음 처리할 알람과 사용자 조건
     */
    @Transactional
    public boolean execute(MarkAlarmReadInput input) {
        return userAlarmRepository.findByAlarmIdAndHandle(input.getAlarmId(), input.getHandle())
                .map(this::markReadIfUnread)
                .orElse(false);
    }

    private boolean markReadIfUnread(UserAlarm alarm) {
        // 미확인 알람이면 읽음 상태로 변경
        if (!alarm.isRead()) {
            alarm.markRead();
        }

        return true;
    }
}

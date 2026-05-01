package com.quertimizer.alarm.application.usecase;

import com.quertimizer.alarm.application.port.UserAlarmRepository;
import com.quertimizer.alarm.domain.entity.UserAlarm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MarkAllAlarmsRead {

    private final UserAlarmRepository userAlarmRepository;

    /**
     * 사용자 알람을 모두 읽음 처리한다.
     *
     * <ol>
     *   <li>사용자 미확인 알람 조회
     *   <li>미확인 알람 전체 읽음 처리
     * </ol>
     *
     * @param handle 읽음 처리할 사용자 handle
     */
    @Transactional
    public void execute(String handle) {
        List<UserAlarm> unreadAlarms = userAlarmRepository.findAllByHandleAndReadFalseOrderByCreatedAtDescAlarmIdDesc(handle);
        if (unreadAlarms.isEmpty()) {
            return;
        }

        unreadAlarms.forEach(UserAlarm::markRead);
    }
}

package com.quertimizer.alarm.adapter.out.user;

import com.quertimizer.alarm.application.port.out.AlarmRecipientPort;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AlarmRecipientAdapter implements AlarmRecipientPort {

    private final UserRepositoryPort userRepository;

    @Override
    public List<String> findExistingHandles(List<String> handles) {
        // user 저장소 기준 존재하는 handle 목록 조회
        return userRepository.findAllByHandleIn(handles).stream()
                .map(User::getHandle)
                .filter(handle -> handle != null && !handle.isBlank())
                .distinct()
                .toList();
    }

    @Override
    public List<String> searchHandles(String keyword) {
        // user 저장소 기준 수신자 handle 후보 검색
        return userRepository.findTop20ByHandleContainingIgnoreCaseOrderByHandleAsc(keyword).stream()
                .map(User::getHandle)
                .filter(handle -> handle != null && !handle.isBlank())
                .distinct()
                .toList();
    }

}

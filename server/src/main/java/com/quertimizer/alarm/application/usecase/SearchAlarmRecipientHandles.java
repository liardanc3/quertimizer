package com.quertimizer.alarm.application.usecase;

import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchAlarmRecipientHandles {

    private final UserRepository userRepository;

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
    public List<String> execute(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isBlank()) {
            return List.of();
        }

        return userRepository.findTop20ByHandleContainingIgnoreCaseOrderByHandleAsc(normalizedKeyword).stream()
                .map(User::getHandle)
                .filter(handle -> handle != null && !handle.isBlank())
                .distinct()
                .toList();
    }
}

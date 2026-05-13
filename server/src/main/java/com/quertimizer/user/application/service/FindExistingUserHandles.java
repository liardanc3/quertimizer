package com.quertimizer.user.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.user.application.port.in.FindExistingUserHandlesUseCase;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FindExistingUserHandles implements FindExistingUserHandlesUseCase {

    private final UserRepositoryPort userRepository;

    /**
     * 존재하는 사용자 handle 목록을 조회한다.
     *
     * @param handles 존재 여부를 확인할 사용자 handle 목록
     */
    @Transactional(readOnly = true)
    @Override
    @Log("사용자 Handle 존재 조회")
    public List<String> execute(List<String> handles) {
        return userRepository.findAllByHandleIn(handles).stream()
                .map(User::getHandle)
                .filter(handle -> handle != null && !handle.isBlank())
                .distinct()
                .toList();
    }
}

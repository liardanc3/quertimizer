package com.quertimizer.user.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.user.application.port.in.SearchUserHandlesUseCase;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchUserHandles implements SearchUserHandlesUseCase {

    private final UserRepositoryPort userRepository;

    /**
     * 검색어에 맞는 사용자 handle 후보를 조회한다.
     *
     * @param keyword 사용자 handle 검색어
     */
    @Transactional(readOnly = true)
    @Override
    @Log("사용자 Handle 검색")
    public List<String> execute(String keyword) {
        return userRepository.findTop20ByHandleContainingIgnoreCaseOrderByHandleAsc(keyword).stream()
                .map(User::getHandle)
                .filter(handle -> handle != null && !handle.isBlank())
                .distinct()
                .toList();
    }
}

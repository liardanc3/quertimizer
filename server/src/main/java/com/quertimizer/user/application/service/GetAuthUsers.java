package com.quertimizer.user.application.service;

import com.quertimizer.user.application.output.AuthUserOutput;
import com.quertimizer.user.application.port.in.GetAuthUsersUseCase;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetAuthUsers implements GetAuthUsersUseCase {

    private final UserRepositoryPort userRepository;
    private final AuthUserOutputMapper authUserOutputMapper;

    /**
     * 인증 관리 context에 공개할 사용자 목록을 조회한다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuthUserOutput> execute() {
        return userRepository.findAllByOrderByHandleAsc().stream()
                .map(authUserOutputMapper::toOutput)
                .toList();
    }
}

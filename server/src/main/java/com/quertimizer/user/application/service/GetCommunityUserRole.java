package com.quertimizer.user.application.service;

import com.quertimizer.user.application.port.in.GetCommunityUserRoleUseCase;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetCommunityUserRole implements GetCommunityUserRoleUseCase {

    private final UserRepositoryPort userRepository;

    /**
     * 커뮤니티 context에 공개할 사용자 역할을 조회한다.
     *
     * @param handle 역할 조회 대상 handle
     */
    @Override
    @Transactional(readOnly = true)
    public UserRole execute(String handle) {
        return userRepository.findByHandle(handle)
                .map(user -> user.getResolvedRole())
                .orElse(UserRole.USER);
    }
}

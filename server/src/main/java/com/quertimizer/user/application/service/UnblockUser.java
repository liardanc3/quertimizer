package com.quertimizer.user.application.service;

import com.quertimizer.user.application.port.in.UnblockUserUseCase;
import com.quertimizer.auth.application.port.out.BlockedIpRepositoryPort;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UnblockUser implements UnblockUserUseCase {

    private final UserRepositoryPort userRepository;
    private final BlockedIpRepositoryPort blockedIpRepository;

    /**
     * 사용자 차단을 해제한다.
     *
     * <ol>
     *   <li>사용자 차단 상태 해제
     *   <li>사용자와 연결된 IP 차단 해제
     * </ol>
     *
     * @param handle 차단 해제할 사용자 handle
     */
    @Transactional
    @Override
    public void execute(String handle) {
        userRepository.findByHandle(handle)
                .ifPresent(user -> {
                    user.unblock();
                    userRepository.save(user);
                });
        blockedIpRepository.deleteByBlockedHandle(handle);
    }
}

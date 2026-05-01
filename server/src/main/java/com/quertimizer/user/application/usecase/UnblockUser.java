package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.port.BlockedIpRepository;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UnblockUser {

    private final UserRepository userRepository;
    private final BlockedIpRepository blockedIpRepository;

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
    public void execute(String handle) {
        userRepository.findByHandle(handle).ifPresent(User::unblock);
        blockedIpRepository.deleteByBlockedHandle(handle);
    }
}

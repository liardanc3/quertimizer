package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.port.BlockedIpRepository;
import com.quertimizer.auth.domain.entity.BlockedIp;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.auth.domain.model.AuthFailReason.USER_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class BlockUser {

    private final UserRepository userRepository;
    private final BlockedIpRepository blockedIpRepository;

    /**
     * 사용자를 차단한다.
     *
     * <ol>
     *   <li>차단 대상 사용자 조회
     *   <li>사용자 차단 상태 반영
     *   <li>마지막 접속 IP 차단 반영
     * </ol>
     *
     * @param handle 차단할 사용자 handle
     */
    @Transactional
    public void execute(String handle) {
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        user.block();

        if (user.getLastAccessIp() == null || user.getLastAccessIp().isBlank()) {
            return;
        }

        blockedIpRepository.findById(user.getLastAccessIp().trim())
                .ifPresentOrElse(
                        blockedIp -> blockedIp.refresh(handle),
                        () -> blockedIpRepository.save(BlockedIp.create(user.getLastAccessIp().trim(), handle))
                );
    }
}

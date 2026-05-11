package com.quertimizer.user.application.service;

import com.quertimizer.user.application.port.in.BlockUserUseCase;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.out.UserAccountRestrictionPort;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.user.domain.model.UserProfileFailReason.USER_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class BlockUser implements BlockUserUseCase {

    private final UserRepositoryPort userRepository;
    private final UserAccountRestrictionPort userAccountRestrictionPort;

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
    @Override
    public void execute(String handle) {
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        user.block();
        userRepository.save(user);

        userAccountRestrictionPort.blockIp(user.getLastAccessIp(), handle);
    }
}

package com.quertimizer.user.application.service;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.in.DeleteMyAccountUseCase;
import com.quertimizer.user.application.port.out.UserExternalLinkRepositoryPort;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.quertimizer.user.domain.model.UserProfileFailReason.USER_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class DeleteMyAccount implements DeleteMyAccountUseCase {

    private final UserRepositoryPort userRepository;
    private final UserExternalLinkRepositoryPort userExternalLinkRepository;

    /**
     * 현재 사용자 계정을 삭제한다.
     *
     * <ol>
     *   <li>삭제 대상 사용자 조회
     *   <li>사용자 외부 링크 삭제
     *   <li>사용자 계정 삭제
     * </ol>
     *
     * @param email 삭제할 사용자 이메일
     */
    @Transactional
    @Override
    public void execute(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        Optional.ofNullable(user.getHandle())
                .filter(handle -> !handle.isBlank())
                .ifPresent(userExternalLinkRepository::deleteAllByIdHandle);

        userRepository.deleteByEmail(user.getEmail());
    }
}

package com.quertimizer.user.application.service;

import com.quertimizer.user.application.input.AuthUserLookupInput;
import com.quertimizer.user.application.output.AuthUserOutput;
import com.quertimizer.user.application.port.in.GetAuthUserUseCase;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetAuthUser implements GetAuthUserUseCase {

    private final UserRepositoryPort userRepository;
    private final AuthUserOutputMapper authUserOutputMapper;

    /**
     * 인증 context에 공개할 사용자 정보를 조회한다.
     *
     * <ol>
     *   <li>조회 유형별 사용자 조회
     *   <li>인증 전용 사용자 응답 변환
     * </ol>
     *
     * @param input 인증 사용자 조회 조건
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUserOutput> execute(AuthUserLookupInput input) {
        return findUser(input).map(authUserOutputMapper::toOutput);
    }

    private Optional<User> findUser(AuthUserLookupInput input) {
        // 조회 유형별 사용자 조회
        return switch (input.getType()) {
            case ID -> userRepository.findById(input.getValue());
            case EMAIL -> userRepository.findByEmail(input.getValue());
            case EMAIL_IGNORE_CASE -> userRepository.findByEmailIgnoreCase(input.getValue());
            case HANDLE -> userRepository.findByHandle(input.getValue());
        };
    }
}

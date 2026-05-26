package com.quertimizer.user.application.service;

import com.quertimizer.user.application.input.AuthUserSaveInput;
import com.quertimizer.user.application.output.AuthUserOutput;
import com.quertimizer.user.application.port.in.SaveAuthUserUseCase;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SaveAuthUser implements SaveAuthUserUseCase {

    private final UserRepositoryPort userRepository;
    private final AuthUserOutputMapper authUserOutputMapper;

    /**
     * 인증 context에서 변경한 사용자 인증 상태를 저장한다.
     *
     * <ol>
     *   <li>기존 사용자 조회
     *   <li>인증 관련 상태 반영
     *   <li>저장된 사용자 응답 반환
     * </ol>
     *
     * @param input 저장할 인증 사용자 상태
     */
    @Override
    @Transactional
    public AuthUserOutput execute(AuthUserSaveInput input) {
        User user = userRepository.findById(input.getEmail())
                .map(existingUser -> restoreUser(existingUser, input))
                .orElseGet(() -> createUser(input));
        return authUserOutputMapper.toOutput(userRepository.save(user));
    }

    private User createUser(AuthUserSaveInput input) {
        // 기존 user 도메인 사용자가 없으면 auth 사용자 기본값으로 신규 생성
        User newUser = hasHandle(input)
                ? User.create(input.getHandle(), input.getPassword(), input.getEmail())
                : User.create(input.getPassword(), input.getEmail());
        newUser.changeRole(input.getRole());
        newUser.updateLastAccess(input.getLastAccessIp(), input.getLastAccessAt());
        return newUser;
    }

    private User restoreUser(User existingUser, AuthUserSaveInput input) {
        // 기존 user 도메인 사용자 정보는 유지하고 auth 관련 상태만 반영
        return User.restore(
                existingUser.getEmail(), input.getHandle(), input.getPassword(),
                existingUser.getBio(), existingUser.getProfileImageUrl(),
                existingUser.getBackgroundImageUrl(), input.getRole(), existingUser.getDefaultDbms(),
                existingUser.getSqlPublic(), existingUser.getExecutionPercentilePublic(),
                existingUser.getSolvedRecordsPublic(), existingUser.getSolvedProblemCountPublic(),
                existingUser.getCommunityActivityPublic(), existingUser.getSolvedProblemCount(),
                existingUser.getSolvedExecutionTimeSumMs(), existingUser.getSignupAt(),
                input.getLastAccessIp(), input.getLastAccessAt(),
                input.isBlocked(), input.getBlockedAt()
        );
    }

    private boolean hasHandle(AuthUserSaveInput input) {
        // handle 설정 여부 확인
        return input.getHandle() != null && !input.getHandle().isBlank();
    }
}

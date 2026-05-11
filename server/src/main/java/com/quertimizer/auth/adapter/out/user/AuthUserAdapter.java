package com.quertimizer.auth.adapter.out.user;

import com.quertimizer.auth.application.port.out.AuthUserPort;
import com.quertimizer.auth.domain.model.AuthUser;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthUserAdapter implements AuthUserPort {

    private final UserRepositoryPort userRepository;

    @Override
    public Optional<AuthUser> findById(String email) {
        // user 저장소 기준 이메일 식별자 조회
        return userRepository.findById(email).map(this::toAuthUser);
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        // user 저장소 기준 이메일 조회
        return userRepository.findByEmail(email).map(this::toAuthUser);
    }

    @Override
    public Optional<AuthUser> findByEmailIgnoreCase(String email) {
        // user 저장소 기준 대소문자 무시 이메일 조회
        return userRepository.findByEmailIgnoreCase(email).map(this::toAuthUser);
    }

    @Override
    public Optional<AuthUser> findByHandle(String handle) {
        // user 저장소 기준 handle 조회
        return userRepository.findByHandle(handle).map(this::toAuthUser);
    }

    @Override
    public List<AuthUser> findAllByOrderByHandleAsc() {
        // user 저장소 기준 handle 오름차순 사용자 목록 조회
        return userRepository.findAllByOrderByHandleAsc().stream()
                .map(this::toAuthUser)
                .toList();
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        // user 저장소 기준 대소문자 무시 이메일 존재 여부 확인
        return userRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByHandle(String handle) {
        // user 저장소 기준 handle 존재 여부 확인
        return userRepository.existsByHandle(handle);
    }

    @Override
    public AuthUser save(AuthUser user) {
        // user 저장소 기준 사용자 저장
        return toAuthUser(userRepository.save(toUser(userRepository.findById(user.getEmail()).orElse(null), user)));
    }

    private AuthUser toAuthUser(User user) {
        // user 도메인 사용자를 auth 도메인 모델로 변환
        return new AuthUser(
                user.getEmail(), user.getHandle(), user.getPassword(), user.getResolvedRole(),
                user.getResolvedDefaultDbms(), user.getLastAccessIp(), user.getLastAccessAt(),
                user.getBlockedUser(), user.getBlockedAt()
        );
    }

    private User toUser(User existingUser, AuthUser user) {
        // 기존 user 도메인 사용자가 없으면 auth 사용자 기본값으로 신규 생성
        if (existingUser == null) {
            User newUser = user.hasHandle()
                    ? User.create(user.getHandle(), user.getPassword(), user.getEmail())
                    : User.create(user.getPassword(), user.getEmail());
            newUser.changeRole(user.getResolvedRole());
            newUser.updateLastAccess(user.getLastAccessIp(), user.getLastAccessAt());
            return newUser;
        }

        // 기존 user 도메인 사용자 정보는 유지하고 auth 관련 상태만 반영
        return User.restore(
                existingUser.getEmail(), user.getHandle(), user.getPassword(),
                existingUser.getBio(), existingUser.getProfileImageUrl(),
                existingUser.getBackgroundImageUrl(), user.getResolvedRole(), existingUser.getDefaultDbms(),
                existingUser.getSqlPublic(), existingUser.getExecutionPercentilePublic(),
                existingUser.getSolvedRecordsPublic(), existingUser.getSolvedProblemCountPublic(),
                existingUser.getCommunityActivityPublic(), existingUser.getSolvedProblemCount(),
                existingUser.getSolvedExecutionTimeSumMs(), existingUser.getSignupAt(),
                user.getLastAccessIp(), user.getLastAccessAt(),
                user.isBlocked(), user.getBlockedAt()
        );
    }

}

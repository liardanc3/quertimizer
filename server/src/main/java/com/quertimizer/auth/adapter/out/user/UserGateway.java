package com.quertimizer.auth.adapter.out.user;

import com.quertimizer.auth.application.port.out.AuthUserPort;
import com.quertimizer.auth.domain.model.AuthUser;
import com.quertimizer.user.application.input.AuthUserAvailabilityInput;
import com.quertimizer.user.application.input.AuthUserLookupInput;
import com.quertimizer.user.application.input.AuthUserSaveInput;
import com.quertimizer.user.application.output.AuthUserOutput;
import com.quertimizer.user.application.port.in.CheckAuthUserAvailabilityUseCase;
import com.quertimizer.user.application.port.in.GetAuthUserUseCase;
import com.quertimizer.user.application.port.in.GetAuthUsersUseCase;
import com.quertimizer.user.application.port.in.SaveAuthUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("authUserGateway")
@RequiredArgsConstructor
public class UserGateway implements AuthUserPort {

    private final GetAuthUserUseCase getAuthUser;
    private final GetAuthUsersUseCase getAuthUsers;
    private final CheckAuthUserAvailabilityUseCase checkAuthUserAvailability;
    private final SaveAuthUserUseCase saveAuthUser;

    @Override
    public Optional<AuthUser> findById(String email) {
        // user 공개 use case 기준 이메일 식별자 조회
        return getAuthUser.execute(new AuthUserLookupInput(AuthUserLookupInput.Type.ID, email))
                .map(this::toAuthUser);
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        // user 공개 use case 기준 이메일 조회
        return getAuthUser.execute(new AuthUserLookupInput(AuthUserLookupInput.Type.EMAIL, email))
                .map(this::toAuthUser);
    }

    @Override
    public Optional<AuthUser> findByEmailIgnoreCase(String email) {
        // user 공개 use case 기준 대소문자 무시 이메일 조회
        return getAuthUser.execute(new AuthUserLookupInput(AuthUserLookupInput.Type.EMAIL_IGNORE_CASE, email))
                .map(this::toAuthUser);
    }

    @Override
    public Optional<AuthUser> findByHandle(String handle) {
        // user 공개 use case 기준 handle 조회
        return getAuthUser.execute(new AuthUserLookupInput(AuthUserLookupInput.Type.HANDLE, handle))
                .map(this::toAuthUser);
    }

    @Override
    public List<AuthUser> findAllByOrderByHandleAsc() {
        // user 공개 use case 기준 handle 오름차순 사용자 목록 조회
        return getAuthUsers.execute().stream()
                .map(this::toAuthUser)
                .toList();
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        // user 공개 use case 기준 대소문자 무시 이메일 존재 여부 확인
        return checkAuthUserAvailability.execute(new AuthUserAvailabilityInput(email, null)).isEmailExists();
    }

    @Override
    public boolean existsByHandle(String handle) {
        // user 공개 use case 기준 handle 존재 여부 확인
        return checkAuthUserAvailability.execute(new AuthUserAvailabilityInput(null, handle)).isHandleExists();
    }

    @Override
    public AuthUser save(AuthUser user) {
        // user 공개 use case 기준 사용자 저장
        return toAuthUser(saveAuthUser.execute(new AuthUserSaveInput(
                user.getEmail(), user.getHandle(), user.getPassword(), user.getResolvedRole(),
                user.getResolvedDefaultDbms(), user.getLastAccessIp(), user.getLastAccessAt(),
                user.isBlocked(), user.getBlockedAt()
        )));
    }

    private AuthUser toAuthUser(AuthUserOutput output) {
        // user 공개 응답을 auth 도메인 모델로 변환
        return new AuthUser(
                output.getEmail(), output.getHandle(), output.getPassword(), output.getRole(),
                output.getDefaultDbms(), output.getLastAccessIp(), output.getLastAccessAt(),
                output.isBlocked(), output.getBlockedAt()
        );
    }
}

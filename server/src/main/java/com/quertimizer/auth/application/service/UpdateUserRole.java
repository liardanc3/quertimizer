package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.UpdateUserRoleUseCase;
import com.quertimizer.auth.application.input.UpdateUserRoleInput;
import com.quertimizer.auth.domain.policy.AuthManagePolicy;
import com.quertimizer.auth.application.service.AuthManageService;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateUserRole implements UpdateUserRoleUseCase {

    private final AuthManageService authManageService;
    private final AuthManagePolicy authManagePolicy;
    private final UserRepositoryPort userRepository;

    /**
     * 사용자 역할을 수정한다.
     *
     * <ol>
     *   <li>변경 대상 사용자와 다음 역할 확정
     *   <li>민감 작업 확인과 관리자 보호 정책 검증
     *   <li>사용자 역할 변경
     * </ol>
     *
     * @param input 역할 변경 대상과 다음 역할 입력
     */
    @Transactional
    @Override
    public void execute(UpdateUserRoleInput input) {
        User user = authManageService.findUser(input.getHandle());
        UserRole nextRole = authManageService.normalizeRole(input.getRole());
        UserRole currentRole = user.getResolvedRole();
        long adminCount = userRepository.findAllByOrderByHandleAsc().stream()
                .filter(storedUser -> storedUser.getResolvedRole() == UserRole.ADMIN)
                .count();

        authManagePolicy.validateSensitiveConfirmation(input.getConfirmationText());
        authManagePolicy.validateSelfAdminRemoval(input.getActorEmail(), user.getEmail(), currentRole, nextRole);
        authManagePolicy.validateAdminRoleChange(currentRole, nextRole, adminCount);

        user.changeRole(nextRole);
        userRepository.save(user);

        log.info("Auth role changed actor={} target={} before={} after={}",
                input.getActorEmail(), input.getHandle(), currentRole, nextRole);
    }
}

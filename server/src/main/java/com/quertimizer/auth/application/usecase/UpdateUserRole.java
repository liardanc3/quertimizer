package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.UpdateUserRoleInput;
import com.quertimizer.auth.domain.policy.AuthManagePolicy;
import com.quertimizer.auth.application.service.AuthManageService;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.problem.application.port.ProblemGeneratorPermissionRepository;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateUserRole {

    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;
    private final AuthManageService authManageService;
    private final AuthManagePolicy authManagePolicy;

    /**
     * 사용자 역할을 수정한다.
     *
     * <ol>
     *   <li>변경 대상 사용자와 다음 역할 확정
     *   <li>민감 작업 확인과 관리자 보호 정책 검증
     *   <li>사용자 역할 변경과 불필요 권한 정리
     * </ol>
     *
     * @param input 역할 변경 대상과 다음 역할 입력
     */
    @Transactional
    public void execute(UpdateUserRoleInput input) {
        User user = authManageService.findUser(input.getHandle());
        UserRole nextRole = authManageService.normalizeRole(input.getRole());
        UserRole currentRole = user.getResolvedRole();

        authManagePolicy.validateSensitiveConfirmation(input.getConfirmationText());
        authManagePolicy.validateSelfAdminRemoval(input.getActorEmail(), user.getEmail(), currentRole, nextRole);
        authManagePolicy.validateAdminRoleChange(currentRole, nextRole);

        user.changeRole(nextRole);
        if (nextRole != UserRole.PROBLEM_GENERATOR) {
            problemGeneratorPermissionRepository.deleteAllByIdHandle(input.getHandle());
        }

        log.info("Auth role changed actor={} target={} before={} after={}",
                input.getActorEmail(), input.getHandle(), currentRole, nextRole);
    }
}

package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.UpdateProblemGeneratorPermissionsInput;
import com.quertimizer.auth.domain.policy.AuthManagePolicy;
import com.quertimizer.auth.application.service.AuthManageService;
import com.quertimizer.problem.application.port.ProblemGeneratorPermissionRepository;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UpdateProblemGeneratorPermissions {

    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;
    private final AuthManageService authManageService;
    private final AuthManagePolicy authManagePolicy;

    /**
     * ProblemGenerator 문제 권한을 수정한다.
     *
     * <ol>
     *   <li>ProblemGenerator 사용자 여부 검증
     *   <li>저장할 권한 키 정규화와 유효성 검증
     *   <li>기존 권한 삭제 후 신규 권한 저장
     * </ol>
     *
     * @param input 권한 변경 대상과 교체할 권한 목록 입력
     */
    @Transactional
    public void execute(UpdateProblemGeneratorPermissionsInput input) {
        User user = authManageService.findUser(input.getHandle());
        authManagePolicy.validateProblemGeneratorRole(user.getResolvedRole());

        List<String> normalizedPermissionKeys = authManageService.normalizePermissionKeys(input.getPermissionKeys());
        authManageService.validatePermissionKeys(normalizedPermissionKeys);

        problemGeneratorPermissionRepository.deleteAllByIdHandle(input.getHandle());
        if (!normalizedPermissionKeys.isEmpty()) {
            problemGeneratorPermissionRepository.saveAll(normalizedPermissionKeys.stream()
                    .map(permissionKey -> ProblemGeneratorPermission.create(input.getHandle(), permissionKey))
                    .toList());
        }
    }
}

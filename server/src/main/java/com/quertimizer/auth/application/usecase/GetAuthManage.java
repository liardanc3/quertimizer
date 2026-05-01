package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.output.AuthManageOutput;
import com.quertimizer.auth.application.output.AuthManageUserRowOutput;
import com.quertimizer.auth.application.service.AuthManageService;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.problem.application.port.ProblemGeneratorPermissionRepository;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetAuthManage {

    private final UserRepository userRepository;
    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;
    private final AuthManageService authManageService;

    /**
     * 권한 설정 화면 데이터를 조회한다.
     *
     * <ol>
     *   <li>사용자와 문제 권한 목록 조회
     *   <li>사용자별 권한 설정 행 응답 조립
     * </ol>
     */
    @Transactional(readOnly = true)
    public AuthManageOutput execute() {
        Map<String, List<String>> permissionMap = createPermissionMap();
        List<AuthManageUserRowOutput> members = userRepository.findAllByOrderByHandleAsc().stream()
                .filter(user -> user.hasHandle())
                .map(user -> new AuthManageUserRowOutput(
                        user.getHandle(), authManageService.resolveRoleValue(user.getResolvedRole()),
                        user.getResolvedRole() == UserRole.PROBLEM_GENERATOR
                                ? authManageService.sortPermissionKeys(permissionMap.getOrDefault(user.getHandle(), List.of()))
                                : List.of()
                ))
                .toList();
        return new AuthManageOutput(members);
    }

    private Map<String, List<String>> createPermissionMap() {
        // 저장된 문제 생성 권한을 사용자별 map으로 구성
        return problemGeneratorPermissionRepository.findAllByOrderByIdHandleAscIdProblemIdAsc().stream()
                .collect(Collectors.groupingBy(
                        ProblemGeneratorPermission::getHandle,
                        Collectors.mapping(permission -> authManageService.normalizeStoredPermissionKey(permission.getProblemId()), Collectors.toList())
                ));
    }
}

package com.quertimizer.problem.application.usecase;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.problem.application.output.ProblemSetSummaryOutput;
import com.quertimizer.problem.application.service.ProblemService;
import com.quertimizer.problem.application.store.ProblemStore;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.policy.ProblemManagementPolicy;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetProblemSets {

    private final ProblemStore problemStore;
    private final ProblemService problemService;
    private final ProblemManagementPolicy problemManagementPolicy;

    /**
     * 문제 테이블셋 목록을 조회한다.
     *
     * <ol>
     *   <li>문제 관리 사용자 권한 확인
     *   <li>관리자 전체 테이블셋 조회
     *   <li>문제 생성자 권한 범위 테이블셋 조회
     * </ol>
     *
     * @param authenticatedEmail 조회 권한을 확인할 인증 이메일
     */
    public List<ProblemSetSummaryOutput> execute(String authenticatedEmail) {
        User currentUser = problemService.requireProblemManagementUser(authenticatedEmail);

        if (currentUser.getResolvedRole() == UserRole.ADMIN) {
            return problemStore.findAllProblemSets().stream()
                    .map(ProblemSet::getProblemSetId)
                    .distinct()
                    .sorted()
                    .map(ProblemSetSummaryOutput::new)
                    .toList();
        }

        return problemService.findPermissionKeys(currentUser.getHandle()).stream()
                .map(this::toProblemSetPermissionKey)
                .filter(permissionKey -> !permissionKey.isBlank())
                .distinct()
                .sorted()
                .map(ProblemSetSummaryOutput::new)
                .toList();
    }

    private String toProblemSetPermissionKey(String permissionKey) {
        // 권한 키에서 문제 테이블셋 범위 추출
        if (problemManagementPolicy.isScopedProblemSetId(permissionKey)) {
            return permissionKey;
        }

        if (problemManagementPolicy.isScopedProblemId(permissionKey)) {
            return permissionKey.split("-")[0];
        }

        return "";
    }
}

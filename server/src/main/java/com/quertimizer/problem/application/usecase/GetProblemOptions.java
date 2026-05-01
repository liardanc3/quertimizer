package com.quertimizer.problem.application.usecase;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.problem.application.input.ProblemOptionsInput;
import com.quertimizer.problem.application.output.AdminProblemOptionOutput;
import com.quertimizer.problem.application.port.ProblemRepository;
import com.quertimizer.problem.application.service.ProblemService;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GetProblemOptions {

    private final ProblemRepository problemRepository;
    private final ProblemService problemService;

    /**
     * 문제 관리용 문제 옵션 목록을 조회한다.
     *
     * <ol>
     *   <li>문제 관리 사용자 권한 확인
     *   <li>문제 테이블셋 접근 검증
     *   <li>권한 범위 기준 문제 옵션 조회
     * </ol>
     *
     * @param input 옵션을 조회할 문제 테이블셋과 인증 이메일 입력
     */
    public List<AdminProblemOptionOutput> execute(ProblemOptionsInput input) {
        User currentUser = problemService.requireProblemManagementUser(input.getAuthenticatedEmail());

        String scopedProblemSetId = problemService.normalizeScopedProblemSetId(input.getProblemSetId(), null);
        problemService.validateProblemSetAccess(currentUser, scopedProblemSetId);

        List<Problem> problems = problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId);
        if (currentUser.getResolvedRole() == UserRole.ADMIN) {
            return toAdminProblemOptions(problems);
        }

        Set<String> permissionKeys = problemService.findPermissionKeys(currentUser.getHandle());
        if (permissionKeys.contains(scopedProblemSetId)) {
            return toAdminProblemOptions(problems);
        }

        return problems.stream()
                .filter(problem -> permissionKeys.contains(problem.getProblemId()))
                .map(problem -> new AdminProblemOptionOutput(problem.getProblemId()))
                .toList();
    }

    private List<AdminProblemOptionOutput> toAdminProblemOptions(List<Problem> problems) {
        // 문제 엔티티 목록을 관리자 옵션 응답으로 변환
        return problems.stream()
                .map(problem -> new AdminProblemOptionOutput(problem.getProblemId()))
                .toList();
    }
}

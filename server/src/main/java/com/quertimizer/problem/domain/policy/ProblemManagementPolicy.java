package com.quertimizer.problem.domain.policy;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.domain.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.NEW_PROBLEM_SET_PERMISSION_REQUIRED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_ACCESS_DENIED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_MANAGEMENT_ACCESS_DENIED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_SET_ACCESS_DENIED;
import static com.quertimizer.problem.domain.model.ProblemPermissionKey.NEW;

@Component
public class ProblemManagementPolicy {

    private static final Pattern RAW_SCOPED_PROBLEM_ID_PATTERN = Pattern.compile("^\\d{5}-\\d{5}$");
    private static final Pattern RAW_SCOPED_PROBLEM_SET_ID_PATTERN = Pattern.compile("^\\d{5}$");

    /**
     * 문제 관리 기능을 사용할 수 있는 사용자 역할인지 검증한다.
     *
     * @param user 검증할 사용자
     */
    public void validateProblemManagementUser(User user) {
        if (user.getResolvedRole() != UserRole.ADMIN && user.getResolvedRole() != UserRole.PROBLEM_GENERATOR) {
            throw new BusinessException(PROBLEM_MANAGEMENT_ACCESS_DENIED.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * 문제 테이블셋 접근 권한을 검증한다.
     *
     * <ol>
     *   <li>관리자 권한 확인
     *   <li>테이블셋 또는 하위 문제 권한 확인
     * </ol>
     *
     * @param currentUser 현재 인증 사용자
     * @param permissionKeys 현재 사용자가 가진 문제 관리 권한 키
     * @param scopedProblemSetId 접근할 문제 테이블셋 번호
     */
    public void validateProblemSetAccess(User currentUser, Set<String> permissionKeys, String scopedProblemSetId) {
        if (currentUser.getResolvedRole() == UserRole.ADMIN) {
            return;
        }

        boolean hasProblemAccess = permissionKeys.stream()
                .anyMatch(permissionKey -> isScopedProblemId(permissionKey) && permissionKey.startsWith(scopedProblemSetId + "-"));
        if (permissionKeys.contains(scopedProblemSetId) || hasProblemAccess) {
            return;
        }

        throw new BusinessException(PROBLEM_SET_ACCESS_DENIED.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * 문제 또는 문제셋 생성/수정 권한을 검증한다.
     *
     * <ol>
     *   <li>관리자 권한 확인
     *   <li>신규 문제셋 생성 권한 확인
     *   <li>기존 문제 또는 기존 문제셋 권한 확인
     * </ol>
     *
     * @param currentUser 현재 인증 사용자
     * @param permissionKeys 현재 사용자가 가진 문제 관리 권한 키
     * @param useExistingProblemSet 기존 문제셋 사용 여부
     * @param useExistingProblem 기존 문제 수정 여부
     * @param scopedProblemSetId 생성 또는 수정 대상 문제셋 번호
     * @param problemId 수정 대상 문제 번호
     */
    public void validateProblemWriteAccess(User currentUser,
                                           Set<String> permissionKeys,
                                           boolean useExistingProblemSet,
                                           boolean useExistingProblem,
                                           String scopedProblemSetId,
                                           String problemId) {
        if (currentUser.getResolvedRole() == UserRole.ADMIN) {
            return;
        }

        if (!useExistingProblemSet) {
            if (!permissionKeys.contains(NEW.getValue())) {
                throw new BusinessException(NEW_PROBLEM_SET_PERMISSION_REQUIRED.getMessage(), HttpStatus.FORBIDDEN);
            }
            return;
        }

        if (useExistingProblem) {
            if (permissionKeys.contains(scopedProblemSetId) || permissionKeys.contains(problemId)) {
                return;
            }

            throw new BusinessException(PROBLEM_ACCESS_DENIED.getMessage(), HttpStatus.FORBIDDEN);
        }

        if (!permissionKeys.contains(scopedProblemSetId)) {
            throw new BusinessException(PROBLEM_SET_ACCESS_DENIED.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * 문제 관리 권한 키를 저장 형식으로 정규화한다.
     *
     * @param permissionKey 정규화할 권한 키
     * @return 저장 형식 권한 키
     */
    public String normalizePermissionKey(String permissionKey) {
        if (permissionKey == null || permissionKey.isBlank()) {
            return "";
        }

        String normalizedPermissionKey = permissionKey.trim().toUpperCase(Locale.ROOT);
        if (NEW.getValue().equals(normalizedPermissionKey)) {
            return NEW.getValue();
        }

        if (RAW_SCOPED_PROBLEM_ID_PATTERN.matcher(normalizedPermissionKey).matches()) {
            return "P" + normalizedPermissionKey;
        }

        if (RAW_SCOPED_PROBLEM_SET_ID_PATTERN.matcher(normalizedPermissionKey).matches()) {
            return "P" + normalizedPermissionKey;
        }

        return normalizedPermissionKey;
    }

    /**
     * 스코프가 포함된 문제 테이블셋 번호인지 확인한다.
     *
     * @param problemSetId 확인할 문제 테이블셋 번호
     * @return 스코프 문제 테이블셋 번호 여부
     */
    public boolean isScopedProblemSetId(String problemSetId) {
        return DbmsType.isScopedProblemSetId(problemSetId);
    }

    /**
     * 스코프가 포함된 문제 번호인지 확인한다.
     *
     * @param permissionKey 확인할 권한 키 또는 문제 번호
     * @return 스코프 문제 번호 여부
     */
    public boolean isScopedProblemId(String permissionKey) {
        return DbmsType.isScopedProblemId(permissionKey);
    }

}

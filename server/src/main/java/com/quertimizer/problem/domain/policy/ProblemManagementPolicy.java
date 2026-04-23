package com.quertimizer.problem.domain.policy;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.domain.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.NEW_PROBLEM_SET_PERMISSION_REQUIRED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_ACCESS_DENIED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_MANAGEMENT_ACCESS_DENIED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_SET_ACCESS_DENIED;
import static com.quertimizer.problem.domain.model.ProblemPermissionKey.NEW;

@Component
public class ProblemManagementPolicy {

    public void validateProblemManagementUser(User user) {
        if (user.getResolvedRole() != UserRole.ADMIN && user.getResolvedRole() != UserRole.PROBLEM_GENERATOR) {
            throw new BusinessException(PROBLEM_MANAGEMENT_ACCESS_DENIED.getMessage(), HttpStatus.FORBIDDEN);
        }
    }

    public void validateProblemSetAccess(User currentUser, Set<String> permissionKeys, String scopedProblemSetId) {
        if (currentUser.getResolvedRole() == UserRole.ADMIN) {
            return;
        }

        if (permissionKeys.contains(scopedProblemSetId)
                || permissionKeys.stream().anyMatch(permissionKey -> isScopedProblemId(permissionKey) && permissionKey.startsWith(scopedProblemSetId + "-"))) {
            return;
        }

        throw new BusinessException(PROBLEM_SET_ACCESS_DENIED.getMessage(), HttpStatus.FORBIDDEN);
    }

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

    public String normalizePermissionKey(String permissionKey) {
        if (permissionKey == null || permissionKey.isBlank()) {
            return "";
        }

        String normalizedPermissionKey = permissionKey.trim().toUpperCase();
        if (normalizedPermissionKey.matches("^\\d{5}-\\d{5}$")) {
            return "P" + normalizedPermissionKey;
        }

        if (normalizedPermissionKey.matches("^\\d{5}$")) {
            return "P" + normalizedPermissionKey;
        }

        return normalizedPermissionKey;
    }

    public boolean isScopedProblemSetId(String problemSetId) {
        return problemSetId.matches("^[PO]\\d{5}$");
    }

    public boolean isScopedProblemId(String permissionKey) {
        return permissionKey.matches("^[PO]\\d{5}-\\d{5}$");
    }

}

package com.quertimizer.problem.application.service;

import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.ExecutionPlanElementIndexes;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.application.output.ProblemDetailOutput;
import com.quertimizer.problem.application.output.ProblemListItemOutput;
import com.quertimizer.problem.application.output.ProblemSubmittedHistoryOutput;
import com.quertimizer.problem.application.port.ProblemGeneratorPermissionRepository;
import com.quertimizer.problem.application.store.ProblemStore;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.policy.ProblemManagementPolicy;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.DBMS_REQUIRED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_MANAGEMENT_ACCESS_DENIED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_SET_ID_REQUIRED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;
    private final AuthService authService;
    private final ProblemManagementPolicy problemManagementPolicy;

    public DbmsType resolveDbmsType(String dbms) {
        // 요청 DBMS 값을 내부 유형으로 정규화
        return DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL);
    }

    public User requireProblemManagementUser(String authenticatedEmail) {
        // 인증 사용자 조회 후 문제 관리 검증 및 사용자 반환
        return authService.findAuthenticatedUser(authenticatedEmail)
                .map(user -> {
                    problemManagementPolicy.validateProblemManagementUser(user);
                    return user;
                })
                .orElseThrow(() -> new BusinessException(PROBLEM_MANAGEMENT_ACCESS_DENIED.getMessage(), HttpStatus.FORBIDDEN));
    }

    public void validateProblemSetAccess(User currentUser, String scopedProblemSetId) {
        // 문제 테이블셋 접근 권한 검증
        problemManagementPolicy.validateProblemSetAccess(currentUser, findPermissionKeys(currentUser.getHandle()), scopedProblemSetId);
    }

    public Set<String> findPermissionKeys(String handle) {
        // 문제 생성자 권한 키 조회와 정책 정규화
        return problemGeneratorPermissionRepository.findAllByIdHandleOrderByIdProblemIdAsc(handle).stream()
                .map(ProblemGeneratorPermission::getProblemId)
                .map(problemManagementPolicy::normalizePermissionKey)
                .filter(permissionKey -> !permissionKey.isBlank())
                .collect(Collectors.toSet());
    }

    public String normalizeProblemSetId(String problemSetId) {
        // 문제 테이블셋 번호 필수값 정규화
        return Optional.ofNullable(problemSetId)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new BusinessException(PROBLEM_SET_ID_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST));
    }

    public String normalizeScopedProblemSetId(String problemSetId, DbmsType dbmsType) {
        // 문제 테이블셋 번호 필수값 정규화
        String normalizedProblemSetId = normalizeProblemSetId(problemSetId);

        // 이미 스코프가 포함된 문제 테이블셋 번호 여부 검사
        if (problemManagementPolicy.isScopedProblemSetId(normalizedProblemSetId)) {
            return normalizedProblemSetId;
        }

        // 스코프 생성에 필요한 DBMS 유형 누락 여부 검사
        if (dbmsType == null) {
            throw new BusinessException(DBMS_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // DBMS 스코프 포함 문제 테이블셋 번호 생성 후 반환
        return createProblemSetId(dbmsType, normalizedProblemSetId);
    }

    public DbmsType resolveScopedDbmsType(String scopedId) {
        // 스코프 식별자 기준 DBMS 유형 결정
        return DbmsType.fromScopedId(scopedId).orElse(DbmsType.POSTGRESQL);
    }

    public String createProblemSetId(DbmsType dbmsType, String baseProblemSetId) {
        // DBMS prefix 포함 문제 테이블셋 번호 생성
        return dbmsType.getIdPrefix() + normalizeBaseProblemSetId(baseProblemSetId);
    }

    public String extractBaseProblemSetId(String scopedValue) {
        // 스코프 식별자 null OR 공백 여부 검사
        if (scopedValue == null || scopedValue.isBlank()) {
            return "";
        }

        // 스코프 식별자에서 기준 문제 테이블셋 번호 추출 후 반환
        String[] tokens = scopedValue.split("-");
        String scopedProblemSetId = tokens.length > 0 ? tokens[0] : scopedValue;
        return DbmsType.extractBaseProblemSetId(scopedProblemSetId);
    }

    public String requireText(String value, String message) {
        // 필수 문자열 null 또는 공백 여부 검사
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }

        // 필수 문자열 공백 제거 후 반환
        return value.trim();
    }

    public String normalizeOptionalText(String value) {
        // 선택 문자열 공백 제거와 null 정규화
        return value != null ? value.trim() : "";
    }

    public ProblemListItemOutput toProblemListItemOutput(ProblemStore.ProblemListEntry problemEntry) {
        // 문제 목록 항목과 제출 이력 응답 변환
        return new ProblemListItemOutput(
                problemEntry.problem().getProblemId(), problemEntry.problem().getTitle(),
                problemEntry.problem().getDescription(), problemEntry.totalSubmitCount(),
                problemEntry.successSubmitCount(), problemEntry.spreadRate(),
                problemEntry.submittedHistories().stream().map(this::toProblemSubmittedHistoryOutput).toList()
        );
    }

    public ProblemDetailOutput toProblemDetailOutput(Problem problem, ProblemSet problemSet) {
        // 문제와 문제 테이블셋 조합 기준 상세 응답 변환
        return new ProblemDetailOutput(
                problem.getProblemId(), problem.getTitle(),
                normalizeOptionalText(problem.getDescription()),
                problem.getDbmsType() == DbmsType.POSTGRESQL ? normalizeOptionalText(problem.getDdl()) : "",
                problem.getDbmsType() == DbmsType.MYSQL ? normalizeOptionalText(problem.getDdl()) : "",
                problemSet.getDbmsType() == DbmsType.POSTGRESQL ? normalizeOptionalText(problemSet.getData()) : "",
                problemSet.getDbmsType() == DbmsType.MYSQL ? normalizeOptionalText(problemSet.getData()) : "",
                normalizeOptionalText(problem.getCondition()),
                normalizeOptionalText(problem.getOutput()),
                normalizeOptionalText(problem.getOutputSample()),
                normalizeOptionalText(problem.getSampleDataSql()),
                normalizeAnswerSql(problem), normalizeOptionalText(problem.getAnswer()), problem.getDbmsType().getValue()
        );
    }

    public ProblemDetailOutput toProblemDetailOutput(Problem problem) {
        // 문제 단독 기준 상세 응답 변환
        return new ProblemDetailOutput(
                problem.getProblemId(), problem.getTitle(),
                normalizeOptionalText(problem.getDescription()),
                problem.getDbmsType() == DbmsType.POSTGRESQL ? normalizeOptionalText(problem.getDdl()) : "",
                problem.getDbmsType() == DbmsType.MYSQL ? normalizeOptionalText(problem.getDdl()) : "",
                "",
                "",
                normalizeOptionalText(problem.getCondition()),
                normalizeOptionalText(problem.getOutput()),
                normalizeOptionalText(problem.getOutputSample()),
                normalizeOptionalText(problem.getSampleDataSql()),
                normalizeAnswerSql(problem), normalizeOptionalText(problem.getAnswer()), problem.getDbmsType().getValue()
        );
    }

    private String normalizeBaseProblemSetId(String problemSetId) {
        // 문제 테이블셋 번호 필수값 정규화
        String normalizedProblemSetId = normalizeProblemSetId(problemSetId);

        // DBMS prefix 제거 후 기준 문제 테이블셋 번호 반환
        return DbmsType.extractBaseProblemSetId(normalizedProblemSetId);
    }

    private ProblemSubmittedHistoryOutput toProblemSubmittedHistoryOutput(ProblemSolveHistory history) {
        // 제출 이력 DBMS 유형 기본값 보정
        DbmsType dbmsType = history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;

        // 문제 제출 기록 응답 변환 후 반환
        return new ProblemSubmittedHistoryOutput(
                dbmsType.getValue(), history.getHandle(),
                ExecutionPlanElementIndexes.normalize(dbmsType, history.getExecutionPlanElement()),
                history.getExecutionTimeMs(), history.getCost()
        );
    }

    private String normalizeAnswerSql(Problem problem) {
        // 저장된 정답 SQL 원문 존재 여부 검사
        if (problem.getAnswerSql() != null && !problem.getAnswerSql().isBlank()) {
            return problem.getAnswerSql();
        }

        // 기존 정답 텍스트의 SELECT 계열 SQL 여부 검사
        String legacyAnswer = normalizeOptionalText(problem.getAnswer());
        if (legacyAnswer.toUpperCase().startsWith("SELECT ") || legacyAnswer.toUpperCase().startsWith("WITH ")) {
            return legacyAnswer;
        }

        // 정답 SQL 부재 시 빈 문자열 반환
        return "";
    }
}

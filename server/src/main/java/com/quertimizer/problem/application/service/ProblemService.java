package com.quertimizer.problem.application.service;

import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.problem.application.input.ProblemCreateInput;
import com.quertimizer.problem.application.result.AdminProblemOptionResult;
import com.quertimizer.problem.application.result.ProblemCreateResult;
import com.quertimizer.problem.application.result.ProblemDetailResult;
import com.quertimizer.problem.application.result.ProblemListItemResult;
import com.quertimizer.problem.application.result.ProblemPageResult;
import com.quertimizer.problem.application.result.ProblemSetDetailResult;
import com.quertimizer.problem.application.result.ProblemSetSummaryResult;
import com.quertimizer.problem.application.result.ProblemSubmittedHistoryResult;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.policy.ProblemManagementPolicy;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.infrastructure.repository.ProblemGeneratorPermissionRepository;
import com.quertimizer.problem.infrastructure.repository.ProblemRepository;
import com.quertimizer.problem.infrastructure.repository.ProblemSetRepository;
import com.quertimizer.problem.application.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.DBMS_REQUIRED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.EXISTING_PROBLEM_ID_REQUIRED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_MANAGEMENT_ACCESS_DENIED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_NOT_FOUND;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_NOT_IN_PROBLEM_SET;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_SET_ID_REQUIRED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_SET_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemStore problemStore;
    private final ProblemRepository problemRepository;
    private final ProblemSetRepository problemSetRepository;
    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;
    private final AuthService authService;
    private final ProblemManagementPolicy problemManagementPolicy;

    public ProblemPageResult getProblems(int page,
                                         String query,
                                         String dbms,
                                         String solveState,
                                         String currentHandle,
                                         String solvedCountSort,
                                         String totalSubmitSort,
                                         String successSubmitSort,
                                         String spreadRateSort,
                                         Double spreadRateMin,
                                         Double spreadRateMax) {

        ProblemStore.ProblemPage problemPage = problemStore.findProblemPage(
                page,
                query,
                resolveDbmsType(dbms),
                solveState,
                currentHandle,
                solvedCountSort,
                totalSubmitSort,
                successSubmitSort,
                spreadRateSort,
                spreadRateMin,
                spreadRateMax
        );

        List<ProblemListItemResult> problems = problemPage.problems().stream()
                .map(problemEntry -> new ProblemListItemResult(
                        problemEntry.problem().getProblemId(),
                        problemEntry.problem().getTitle(),
                        problemEntry.problem().getDescription(),
                        problemEntry.totalSubmitCount(),
                        problemEntry.successSubmitCount(),
                        problemEntry.spreadRate(),
                        problemEntry.submittedHistories().stream()
                                .map(this::toProblemSubmittedHistoryResult)
                                .toList()
                ))
                .toList();

        return new ProblemPageResult(
                problemPage.currentPage(),
                problemPage.pageSize(),
                problemPage.totalCount(),
                problemPage.totalPages(),
                problemPage.spreadRateMin(),
                problemPage.spreadRateMax(),
                problems
        );
    }

    public Optional<ProblemDetailResult> getProblem(String problemId) {
        return problemStore.findProblem(problemId)
                .map(problem -> problemStore.findProblemSet(problem.getResolvedProblemSetId())
                        .map(problemSet -> toProblemDetailResult(problem, problemSet))
                        .orElseGet(() -> toProblemDetailResult(problem)));
    }

    public List<ProblemSetSummaryResult> getProblemSets(String authenticatedEmail) {
        User currentUser = requireProblemManagementUser(authenticatedEmail);

        if (currentUser.getResolvedRole() == UserRole.ADMIN) {
            return problemStore.findAllProblemSets().stream()
                    .map(ProblemSet::getProblemSetId)
                    .distinct()
                    .sorted()
                    .map(ProblemSetSummaryResult::new)
                    .toList();
        }

        return findPermissionKeys(currentUser.getHandle()).stream()
                .map(permissionKey -> problemManagementPolicy.isScopedProblemSetId(permissionKey)
                        ? permissionKey
                        : problemManagementPolicy.isScopedProblemId(permissionKey) ? permissionKey.split("-")[0] : "")
                .filter(permissionKey -> !permissionKey.isBlank())
                .distinct()
                .sorted()
                .map(ProblemSetSummaryResult::new)
                .toList();
    }

    public Optional<ProblemSetDetailResult> getProblemSet(String problemSetId, String authenticatedEmail) {
        User currentUser = requireProblemManagementUser(authenticatedEmail);
        String normalizedProblemSetId = normalizeProblemSetId(problemSetId);

        if (problemManagementPolicy.isScopedProblemSetId(normalizedProblemSetId)) {
            validateProblemSetAccess(currentUser, normalizedProblemSetId);
            return problemSetRepository.findById(normalizedProblemSetId)
                    .map(problemSet -> createScopedProblemSetDetail(problemSet));
        }

        ProblemSetPair problemSetPair = findProblemSetPair(normalizedProblemSetId);
        if (problemSetPair.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ProblemSetDetailResult(
                normalizedProblemSetId,
                problemSetPair.postgresqlProblemSet() != null ? normalizeOptionalText(problemSetPair.postgresqlProblemSet().getDdl()) : "",
                problemSetPair.oracleProblemSet() != null ? normalizeOptionalText(problemSetPair.oracleProblemSet().getDdl()) : "",
                problemSetPair.postgresqlProblemSet() != null ? normalizeOptionalText(problemSetPair.postgresqlProblemSet().getData()) : "",
                problemSetPair.oracleProblemSet() != null ? normalizeOptionalText(problemSetPair.oracleProblemSet().getData()) : ""
        ));
    }

    public List<AdminProblemOptionResult> getProblemOptions(String problemSetId, String authenticatedEmail) {
        User currentUser = requireProblemManagementUser(authenticatedEmail);
        String scopedProblemSetId = normalizeScopedProblemSetId(problemSetId, null);
        validateProblemSetAccess(currentUser, scopedProblemSetId);

        if (currentUser.getResolvedRole() == UserRole.ADMIN) {
            return problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId).stream()
                    .map(problem -> new AdminProblemOptionResult(problem.getProblemId()))
                    .toList();
        }

        Set<String> permissionKeys = findPermissionKeys(currentUser.getHandle());
        if (permissionKeys.contains(scopedProblemSetId)) {
            return problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId).stream()
                    .map(problem -> new AdminProblemOptionResult(problem.getProblemId()))
                    .toList();
        }

        return problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId).stream()
                .filter(problem -> permissionKeys.contains(problem.getProblemId()))
                .map(problem -> new AdminProblemOptionResult(problem.getProblemId()))
                .toList();
    }

    @Transactional
    public ProblemCreateResult createProblem(ProblemCreateInput request, String authenticatedEmail) {
        User currentUser = requireProblemManagementUser(authenticatedEmail);
        boolean useExistingProblemSet = "existing".equalsIgnoreCase(request.getProblemSetMode());
        boolean useExistingProblem = "existing".equalsIgnoreCase(request.getProblemMode());
        DbmsType dbmsType = resolveTargetDbmsType(request, useExistingProblemSet, useExistingProblem);
        String scopedProblemSetId = useExistingProblemSet
                ? normalizeScopedProblemSetId(request.getProblemSetId(), dbmsType)
                : createProblemSetId(dbmsType, createNextProblemSetBaseId());
        String scopedDdl = resolveScopedDdl(request, dbmsType);

        validateProblemWriteAccess(currentUser, useExistingProblemSet, useExistingProblem, scopedProblemSetId, request.getProblemId());

        if (useExistingProblemSet) {
            requireExistingProblemSet(scopedProblemSetId);
        } else {
            createProblemSet(scopedProblemSetId, request, dbmsType);
        }

        if (useExistingProblem) {
            return updateProblem(request, scopedProblemSetId, scopedDdl, dbmsType);
        }

        String baseProblemSetId = extractBaseProblemSetId(scopedProblemSetId);
        String problemId = createProblemId(dbmsType, baseProblemSetId, createNextProblemSequence(baseProblemSetId));

        problemRepository.save(Problem.create(
                problemId,
                scopedProblemSetId,
                request.getTitle().trim(),
                request.getDescription().trim(),
                scopedDdl,
                dbmsType == DbmsType.POSTGRESQL,
                dbmsType == DbmsType.ORACLE,
                request.getCondition().trim(),
                request.getOutput().trim(),
                normalizeOptionalText(request.getOutputSample()),
                normalizeOptionalText(request.getAnswer()),
                normalizeOptionalText(request.getAnswerSql())
        ));

        addCreatedProblemPermissionIfNeeded(currentUser, problemId);
        problemStore.loadProblems();
        return new ProblemCreateResult(problemId);
    }

    private DbmsType resolveDbmsType(String dbms) {
        return "oracle".equalsIgnoreCase(dbms) ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
    }

    private DbmsType resolveTargetDbmsType(ProblemCreateInput request,
                                           boolean useExistingProblemSet,
                                           boolean useExistingProblem) {
        if (useExistingProblem && request.getProblemId() != null && !request.getProblemId().isBlank()) {
            return resolveScopedDbmsType(request.getProblemId());
        }

        if (useExistingProblemSet && request.getProblemSetId() != null && !request.getProblemSetId().isBlank()) {
            return resolveScopedDbmsType(request.getProblemSetId());
        }

        return resolveDbmsType(request.getDbms());
    }

    private ProblemSetDetailResult createScopedProblemSetDetail(ProblemSet problemSet) {
        if (problemSet.getDbmsType() == DbmsType.ORACLE) {
            return new ProblemSetDetailResult(
                    problemSet.getProblemSetId(),
                    "",
                    normalizeOptionalText(problemSet.getDdl()),
                    "",
                    normalizeOptionalText(problemSet.getData())
            );
        }

        return new ProblemSetDetailResult(
                problemSet.getProblemSetId(),
                normalizeOptionalText(problemSet.getDdl()),
                "",
                normalizeOptionalText(problemSet.getData()),
                ""
        );
    }

    private ProblemSet requireExistingProblemSet(String problemSetId) {
        return problemSetRepository.findById(problemSetId)
                .orElseThrow(() -> new BusinessException(PROBLEM_SET_NOT_FOUND.getMessage(), HttpStatus.BAD_REQUEST));
    }

    private ProblemSet createProblemSet(String problemSetId, ProblemCreateInput request, DbmsType dbmsType) {
        return problemSetRepository.save(ProblemSet.create(
                problemSetId,
                resolveScopedDdl(request, dbmsType),
                resolveScopedData(request, dbmsType),
                dbmsType == DbmsType.POSTGRESQL,
                dbmsType == DbmsType.ORACLE
        ));
    }

    private ProblemCreateResult updateProblem(ProblemCreateInput request,
                                              String problemSetId,
                                              String scopedDdl,
                                              DbmsType dbmsType) {
        String problemId = requireText(request.getProblemId(), EXISTING_PROBLEM_ID_REQUIRED.getMessage());
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(PROBLEM_NOT_FOUND.getMessage(), HttpStatus.BAD_REQUEST));

        if (!problem.getResolvedProblemSetId().equals(problemSetId)) {
            throw new BusinessException(PROBLEM_NOT_IN_PROBLEM_SET.getMessage(), HttpStatus.BAD_REQUEST);
        }

        problem.changeContent(
                request.getTitle().trim(),
                request.getDescription().trim(),
                scopedDdl,
                dbmsType == DbmsType.POSTGRESQL,
                dbmsType == DbmsType.ORACLE,
                request.getCondition().trim(),
                request.getOutput().trim(),
                normalizeOptionalText(request.getOutputSample()),
                normalizeOptionalText(request.getAnswer()),
                normalizeOptionalText(request.getAnswerSql())
        );

        problemStore.loadProblems();
        return new ProblemCreateResult(problem.getProblemId());
    }

    private ProblemSubmittedHistoryResult toProblemSubmittedHistoryResult(com.quertimizer.problem.domain.entity.ProblemSolveHistory history) {
        DbmsType dbmsType = history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;

        return new ProblemSubmittedHistoryResult(
                dbmsType.getValue(),
                history.getHandle(),
                com.quertimizer.global.constant.ExecutionPlanElementIndexes.normalize(dbmsType, history.getExecutionPlanElement()),
                history.getExecutionTimeMs(),
                history.getCost()
        );
    }

    private ProblemDetailResult toProblemDetailResult(Problem problem, ProblemSet problemSet) {
        return new ProblemDetailResult(
                problem.getProblemId(),
                problem.getTitle(),
                normalizeOptionalText(problem.getDescription()),
                problem.getDbmsType() == DbmsType.POSTGRESQL ? normalizeOptionalText(problem.getDdl()) : "",
                problem.getDbmsType() == DbmsType.ORACLE ? normalizeOptionalText(problem.getDdl()) : "",
                problemSet.getDbmsType() == DbmsType.POSTGRESQL ? normalizeOptionalText(problemSet.getData()) : "",
                problemSet.getDbmsType() == DbmsType.ORACLE ? normalizeOptionalText(problemSet.getData()) : "",
                normalizeOptionalText(problem.getCondition()),
                normalizeOptionalText(problem.getOutput()),
                normalizeOptionalText(problem.getOutputSample()),
                normalizeAnswerSql(problem),
                normalizeOptionalText(problem.getAnswer()),
                problem.getDbmsType().getValue()
        );
    }

    private ProblemDetailResult toProblemDetailResult(Problem problem) {
        return new ProblemDetailResult(
                problem.getProblemId(),
                problem.getTitle(),
                normalizeOptionalText(problem.getDescription()),
                problem.getDbmsType() == DbmsType.POSTGRESQL ? normalizeOptionalText(problem.getDdl()) : "",
                problem.getDbmsType() == DbmsType.ORACLE ? normalizeOptionalText(problem.getDdl()) : "",
                "",
                "",
                normalizeOptionalText(problem.getCondition()),
                normalizeOptionalText(problem.getOutput()),
                normalizeOptionalText(problem.getOutputSample()),
                normalizeAnswerSql(problem),
                normalizeOptionalText(problem.getAnswer()),
                problem.getDbmsType().getValue()
        );
    }

    private String normalizeAnswerSql(Problem problem) {
        if (problem.getAnswerSql() != null && !problem.getAnswerSql().isBlank()) {
            return problem.getAnswerSql();
        }

        return normalizeOptionalText(problem.getAnswer());
    }

    private String resolveScopedDdl(ProblemCreateInput request, DbmsType dbmsType) {
        if (dbmsType == DbmsType.ORACLE) {
            return requireText(request.getDdlOracle(), "Oracle DDL이 필요하다.");
        }

        return requireText(request.getDdlPostgresql(), "PostgreSQL DDL이 필요하다.");
    }

    private String resolveScopedData(ProblemCreateInput request, DbmsType dbmsType) {
        if (dbmsType == DbmsType.ORACLE) {
            return requireText(request.getDataOracle(), "Oracle 데이터 SQL이 필요하다.");
        }

        return requireText(request.getDataPostgresql(), "PostgreSQL 데이터 SQL이 필요하다.");
    }

    private ProblemSetPair findProblemSetPair(String problemSetId) {
        return new ProblemSetPair(
                problemSetRepository.findById(createProblemSetId(DbmsType.POSTGRESQL, problemSetId)).orElse(null),
                problemSetRepository.findById(createProblemSetId(DbmsType.ORACLE, problemSetId)).orElse(null)
        );
    }

    private String createNextProblemSetBaseId() {
        return problemSetRepository.findAll().stream()
                .map(ProblemSet::getBaseProblemSetId)
                .filter(problemSetId -> !problemSetId.isBlank())
                .map(problemSetId -> {
                    try {
                        return Integer.parseInt(problemSetId);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                })
                .max(Comparator.naturalOrder())
                .map(maxValue -> formatFiveDigits(maxValue + 1))
                .orElse("00001");
    }

    private int createNextProblemSequence(String baseProblemSetId) {
        return problemRepository.findAll().stream()
                .map(Problem::getProblemId)
                .filter(problemId -> extractBaseProblemSetId(problemId).equals(baseProblemSetId))
                .map(problemId -> problemId.split("-"))
                .filter(tokens -> tokens.length == 2)
                .map(tokens -> {
                    try {
                        return Integer.parseInt(tokens[1]);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                })
                .max(Comparator.naturalOrder())
                .map(maxValue -> maxValue + 1)
                .orElse(1);
    }

    private String createProblemSetId(DbmsType dbmsType, String baseProblemSetId) {
        return (dbmsType == DbmsType.POSTGRESQL ? "P" : "O") + normalizeBaseProblemSetId(baseProblemSetId);
    }

    private String createProblemId(DbmsType dbmsType, String baseProblemSetId, int sequence) {
        return createProblemSetId(dbmsType, baseProblemSetId) + "-" + formatFiveDigits(sequence);
    }

    private String normalizeProblemSetId(String problemSetId) {
        return Optional.ofNullable(problemSetId)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new BusinessException(PROBLEM_SET_ID_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST));
    }

    private String normalizeScopedProblemSetId(String problemSetId, DbmsType dbmsType) {
        String normalizedProblemSetId = normalizeProblemSetId(problemSetId);
        if (problemManagementPolicy.isScopedProblemSetId(normalizedProblemSetId)) {
            return normalizedProblemSetId;
        }

        if (dbmsType == null) {
            throw new BusinessException(DBMS_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return createProblemSetId(dbmsType, normalizedProblemSetId);
    }

    private DbmsType resolveScopedDbmsType(String scopedId) {
        return scopedId != null && scopedId.trim().startsWith("O") ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
    }

    private String extractBaseProblemSetId(String scopedValue) {
        if (scopedValue == null || scopedValue.isBlank()) {
            return "";
        }

        String[] tokens = scopedValue.split("-");
        String scopedProblemSetId = tokens.length > 0 ? tokens[0] : scopedValue;
        return problemManagementPolicy.isScopedProblemSetId(scopedProblemSetId) ? scopedProblemSetId.substring(1) : scopedProblemSetId;
    }

    private String normalizeBaseProblemSetId(String problemSetId) {
        String normalizedProblemSetId = normalizeProblemSetId(problemSetId);
        return normalizedProblemSetId.matches("^[PO]\\d{5}$") ? normalizedProblemSetId.substring(1) : normalizedProblemSetId;
    }

    private String formatFiveDigits(int value) {
        return "%05d".formatted(value);
    }

    private User requireProblemManagementUser(String authenticatedEmail) {
        User user = authService.findAuthenticatedUser(authenticatedEmail)
                .orElseThrow(() -> new BusinessException(PROBLEM_MANAGEMENT_ACCESS_DENIED.getMessage(), HttpStatus.FORBIDDEN));

        problemManagementPolicy.validateProblemManagementUser(user);
        return user;
    }

    private void validateProblemSetAccess(User currentUser, String scopedProblemSetId) {
        problemManagementPolicy.validateProblemSetAccess(currentUser, findPermissionKeys(currentUser.getHandle()), scopedProblemSetId);
    }

    private void validateProblemWriteAccess(User currentUser,
                                            boolean useExistingProblemSet,
                                            boolean useExistingProblem,
                                            String scopedProblemSetId,
                                            String problemId) {
        problemManagementPolicy.validateProblemWriteAccess(
                currentUser,
                findPermissionKeys(currentUser.getHandle()),
                useExistingProblemSet,
                useExistingProblem,
                scopedProblemSetId,
                problemId == null || problemId.isBlank() ? "" : requireText(problemId, EXISTING_PROBLEM_ID_REQUIRED.getMessage())
        );
    }

    private Set<String> findPermissionKeys(String handle) {
        return problemGeneratorPermissionRepository.findAllByIdHandleOrderByIdProblemIdAsc(handle).stream()
                .map(ProblemGeneratorPermission::getProblemId)
                .map(problemManagementPolicy::normalizePermissionKey)
                .filter(permissionKey -> !permissionKey.isBlank())
                .collect(Collectors.toSet());
    }

    private void addCreatedProblemPermissionIfNeeded(User currentUser, String problemId) {
        if (currentUser.getResolvedRole() != UserRole.PROBLEM_GENERATOR) {
            return;
        }

        boolean hasPermission = problemGeneratorPermissionRepository.findAllByIdHandleOrderByIdProblemIdAsc(currentUser.getHandle()).stream()
                .map(ProblemGeneratorPermission::getProblemId)
                .map(problemManagementPolicy::normalizePermissionKey)
                .anyMatch(problemId::equals);

        if (!hasPermission) {
            problemGeneratorPermissionRepository.save(ProblemGeneratorPermission.create(currentUser.getHandle(), problemId));
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }

        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return value != null ? value.trim() : "";
    }

    private record ProblemSetPair(ProblemSet postgresqlProblemSet, ProblemSet oracleProblemSet) {

        private boolean isEmpty() {
            return postgresqlProblemSet == null && oracleProblemSet == null;
        }
    }
}

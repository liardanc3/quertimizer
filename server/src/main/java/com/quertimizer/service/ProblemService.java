package com.quertimizer.service;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.endpoint.api.dto.request.ProblemCreateReq;
import com.quertimizer.endpoint.api.dto.response.AdminProblemOptionRes;
import com.quertimizer.endpoint.api.dto.response.ProblemCreateRes;
import com.quertimizer.endpoint.api.dto.response.ProblemDetailRes;
import com.quertimizer.endpoint.api.dto.response.ProblemListItemRes;
import com.quertimizer.endpoint.api.dto.response.ProblemPageRes;
import com.quertimizer.endpoint.api.dto.response.ProblemSetDetailRes;
import com.quertimizer.endpoint.api.dto.response.ProblemSetSummaryRes;
import com.quertimizer.endpoint.api.dto.response.ProblemSubmittedHistoryRes;
import com.quertimizer.entity.Problem;
import com.quertimizer.entity.ProblemGeneratorPermission;
import com.quertimizer.entity.ProblemSet;
import com.quertimizer.entity.User;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.repository.ProblemGeneratorPermissionRepository;
import com.quertimizer.repository.ProblemRepository;
import com.quertimizer.repository.ProblemSetRepository;
import com.quertimizer.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemStore problemStore;
    private final ProblemRepository problemRepository;
    private final ProblemSetRepository problemSetRepository;
    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;
    private final UserAccountService userAccountService;

    private static final String PROBLEM_MANAGEMENT_ACCESS_DENIED_MESSAGE = "문제 관리 접근 권한이 없다.";
    private static final String NEW_PROBLEM_SET_PERMISSION_REQUIRED_MESSAGE = "신규 테이블셋 생성 권한이 없다.";
    private static final String PROBLEM_SET_ACCESS_DENIED_MESSAGE = "선택한 테이블셋 접근 권한이 없다.";
    private static final String PROBLEM_ACCESS_DENIED_MESSAGE = "선택한 문제 접근 권한이 없다.";
    private static final String NEW_PERMISSION_KEY = "NEW";

    public ProblemPageRes getProblems(int page,
                                      String query,
                                      String dbms,
                                      String solveState,
                                      String currentUserId,
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
                currentUserId,
                solvedCountSort,
                totalSubmitSort,
                successSubmitSort,
                spreadRateSort,
                spreadRateMin,
                spreadRateMax
        );

        List<ProblemListItemRes> problems = problemPage.problems().stream()
                .map(problemEntry -> ProblemListItemRes.of(
                        problemEntry.problem(),
                        problemEntry.totalSubmitCount(),
                        problemEntry.successSubmitCount(),
                        problemEntry.spreadRate(),
                        problemEntry.submittedHistories().stream()
                                .map(ProblemSubmittedHistoryRes::from)
                                .toList()
                ))
                .toList();

        return new ProblemPageRes(
                problemPage.currentPage(),
                problemPage.pageSize(),
                problemPage.totalCount(),
                problemPage.totalPages(),
                problemPage.spreadRateMin(),
                problemPage.spreadRateMax(),
                problems
        );
    }

    public Optional<ProblemDetailRes> getProblem(String problemId) {
        return problemStore.findProblem(problemId)
                .map(problem -> problemStore.findProblemSet(problem.getResolvedProblemSetId())
                        .map(problemSet -> ProblemDetailRes.from(problem, problemSet))
                        .orElseGet(() -> ProblemDetailRes.from(problem)));
    }

    public List<ProblemSetSummaryRes> getProblemSets(String authenticatedEmail) {
        User currentUser = requireProblemManagementUser(authenticatedEmail);

        if (currentUser.getResolvedRole() == com.quertimizer.constant.UserRole.ADMIN) {
            return problemStore.findAllProblemSets().stream()
                    .map(ProblemSet::getProblemSetId)
                    .distinct()
                    .sorted()
                    .map(ProblemSetSummaryRes::new)
                    .toList();
        }

        return findPermissionKeys(currentUser.getUserId()).stream()
                .map(permissionKey -> isScopedProblemSetId(permissionKey)
                        ? permissionKey
                        : isScopedProblemId(permissionKey) ? permissionKey.split("-")[0] : "")
                .filter(permissionKey -> !permissionKey.isBlank())
                .distinct()
                .sorted()
                .map(ProblemSetSummaryRes::new)
                .toList();
    }

    public Optional<ProblemSetDetailRes> getProblemSet(String problemSetId, String authenticatedEmail) {
        User currentUser = requireProblemManagementUser(authenticatedEmail);
        String normalizedProblemSetId = normalizeProblemSetId(problemSetId);

        if (isScopedProblemSetId(normalizedProblemSetId)) {
            validateProblemSetAccess(currentUser, normalizedProblemSetId);
            return problemSetRepository.findById(normalizedProblemSetId)
                    .map(problemSet -> createScopedProblemSetDetail(problemSet));
        }

        ProblemSetPair problemSetPair = findProblemSetPair(normalizedProblemSetId);
        if (problemSetPair.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(ProblemSetDetailRes.from(normalizedProblemSetId, problemSetPair.postgresqlProblemSet(), problemSetPair.oracleProblemSet()));
    }

    public List<AdminProblemOptionRes> getProblemOptions(String problemSetId, String authenticatedEmail) {
        User currentUser = requireProblemManagementUser(authenticatedEmail);
        String scopedProblemSetId = normalizeScopedProblemSetId(problemSetId, null);
        validateProblemSetAccess(currentUser, scopedProblemSetId);

        if (currentUser.getResolvedRole() == com.quertimizer.constant.UserRole.ADMIN) {
            return problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId).stream()
                    .map(AdminProblemOptionRes::from)
                    .toList();
        }

        Set<String> permissionKeys = findPermissionKeys(currentUser.getUserId());
        if (permissionKeys.contains(scopedProblemSetId)) {
            return problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId).stream()
                    .map(AdminProblemOptionRes::from)
                    .toList();
        }

        return problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId).stream()
                .filter(problem -> permissionKeys.contains(problem.getProblemId()))
                .map(AdminProblemOptionRes::from)
                .toList();
    }

    @Transactional
    public ProblemCreateRes createProblem(ProblemCreateReq request, String authenticatedEmail) {
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
        return new ProblemCreateRes(problemId);
    }

    private DbmsType resolveDbmsType(String dbms) {
        return "oracle".equalsIgnoreCase(dbms) ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
    }

    private DbmsType resolveTargetDbmsType(ProblemCreateReq request,
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

    private ProblemSetDetailRes createScopedProblemSetDetail(ProblemSet problemSet) {
        if (problemSet.getDbmsType() == DbmsType.ORACLE) {
            return new ProblemSetDetailRes(
                    problemSet.getProblemSetId(),
                    "",
                    normalizeOptionalText(problemSet.getDdl()),
                    "",
                    normalizeOptionalText(problemSet.getData())
            );
        }

        return new ProblemSetDetailRes(
                problemSet.getProblemSetId(),
                normalizeOptionalText(problemSet.getDdl()),
                "",
                normalizeOptionalText(problemSet.getData()),
                ""
        );
    }

    private ProblemSet requireExistingProblemSet(String problemSetId) {
        return problemSetRepository.findById(problemSetId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 테이블셋이다.", HttpStatus.BAD_REQUEST));
    }

    private ProblemSet createProblemSet(String problemSetId, ProblemCreateReq request, DbmsType dbmsType) {
        return problemSetRepository.save(ProblemSet.create(
                problemSetId,
                resolveScopedDdl(request, dbmsType),
                resolveScopedData(request, dbmsType),
                dbmsType == DbmsType.POSTGRESQL,
                dbmsType == DbmsType.ORACLE
        ));
    }

    private ProblemCreateRes updateProblem(ProblemCreateReq request,
                                           String problemSetId,
                                           String scopedDdl,
                                           DbmsType dbmsType) {
        String problemId = requireText(request.getProblemId(), "기존 문제 번호가 필요하다.");
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 문제 번호다.", HttpStatus.BAD_REQUEST));

        if (!problem.getResolvedProblemSetId().equals(problemSetId)) {
            throw new BusinessException("선택한 문제 번호가 현재 테이블셋에 속하지 않는다.", HttpStatus.BAD_REQUEST);
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
        return new ProblemCreateRes(problem.getProblemId());
    }

    private String resolveScopedDdl(ProblemCreateReq request, DbmsType dbmsType) {
        if (dbmsType == DbmsType.ORACLE) {
            return requireText(request.getDdlOracle(), "Oracle DDL이 필요하다.");
        }

        return requireText(request.getDdlPostgresql(), "PostgreSQL DDL이 필요하다.");
    }

    private String resolveScopedData(ProblemCreateReq request, DbmsType dbmsType) {
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
                .orElseThrow(() -> new BusinessException("테이블셋 번호가 필요하다.", HttpStatus.BAD_REQUEST));
    }

    private String normalizeScopedProblemSetId(String problemSetId, DbmsType dbmsType) {
        String normalizedProblemSetId = normalizeProblemSetId(problemSetId);
        if (isScopedProblemSetId(normalizedProblemSetId)) {
            return normalizedProblemSetId;
        }

        if (dbmsType == null) {
            throw new BusinessException("DBMS 정보가 필요하다.", HttpStatus.BAD_REQUEST);
        }

        return createProblemSetId(dbmsType, normalizedProblemSetId);
    }

    private boolean isScopedProblemSetId(String problemSetId) {
        return problemSetId.matches("^[PO]\\d{5}$");
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
        return scopedProblemSetId.matches("^[PO]\\d{5}$") ? scopedProblemSetId.substring(1) : scopedProblemSetId;
    }

    private String normalizeBaseProblemSetId(String problemSetId) {
        String normalizedProblemSetId = normalizeProblemSetId(problemSetId);
        return normalizedProblemSetId.matches("^[PO]\\d{5}$") ? normalizedProblemSetId.substring(1) : normalizedProblemSetId;
    }

    private String formatFiveDigits(int value) {
        return "%05d".formatted(value);
    }

    private User requireProblemManagementUser(String authenticatedEmail) {
        User user = userAccountService.findAuthenticatedUser(authenticatedEmail)
                .orElseThrow(() -> new BusinessException(PROBLEM_MANAGEMENT_ACCESS_DENIED_MESSAGE, HttpStatus.FORBIDDEN));

        if (user.getResolvedRole() != com.quertimizer.constant.UserRole.ADMIN
                && user.getResolvedRole() != com.quertimizer.constant.UserRole.PROBLEM_GENERATOR) {
            throw new BusinessException(PROBLEM_MANAGEMENT_ACCESS_DENIED_MESSAGE, HttpStatus.FORBIDDEN);
        }

        return user;
    }

    private void validateProblemSetAccess(User currentUser, String scopedProblemSetId) {
        if (currentUser.getResolvedRole() == com.quertimizer.constant.UserRole.ADMIN) {
            return;
        }

        Set<String> permissionKeys = findPermissionKeys(currentUser.getUserId());
        if (permissionKeys.contains(scopedProblemSetId)
                || permissionKeys.stream().anyMatch(permissionKey -> isScopedProblemId(permissionKey) && permissionKey.startsWith(scopedProblemSetId + "-"))) {
            return;
        }

        throw new BusinessException(PROBLEM_SET_ACCESS_DENIED_MESSAGE, HttpStatus.FORBIDDEN);
    }

    private void validateProblemWriteAccess(User currentUser,
                                            boolean useExistingProblemSet,
                                            boolean useExistingProblem,
                                            String scopedProblemSetId,
                                            String problemId) {
        if (currentUser.getResolvedRole() == com.quertimizer.constant.UserRole.ADMIN) {
            return;
        }

        Set<String> permissionKeys = findPermissionKeys(currentUser.getUserId());

        if (!useExistingProblemSet) {
            if (!permissionKeys.contains(NEW_PERMISSION_KEY)) {
                throw new BusinessException(NEW_PROBLEM_SET_PERMISSION_REQUIRED_MESSAGE, HttpStatus.FORBIDDEN);
            }
            return;
        }

        if (useExistingProblem) {
            String targetProblemId = requireText(problemId, "기존 문제 번호가 필요하다.");
            if (permissionKeys.contains(scopedProblemSetId) || permissionKeys.contains(targetProblemId)) {
                return;
            }

            throw new BusinessException(PROBLEM_ACCESS_DENIED_MESSAGE, HttpStatus.FORBIDDEN);
        }

        if (!permissionKeys.contains(scopedProblemSetId)) {
            throw new BusinessException(PROBLEM_SET_ACCESS_DENIED_MESSAGE, HttpStatus.FORBIDDEN);
        }
    }

    private Set<String> findPermissionKeys(String userId) {
        return problemGeneratorPermissionRepository.findAllByIdUserIdOrderByIdProblemIdAsc(userId).stream()
                .map(ProblemGeneratorPermission::getProblemId)
                .map(this::normalizePermissionKey)
                .filter(permissionKey -> !permissionKey.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalizePermissionKey(String permissionKey) {
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

    private boolean isScopedProblemId(String permissionKey) {
        return permissionKey.matches("^[PO]\\d{5}-\\d{5}$");
    }

    private void addCreatedProblemPermissionIfNeeded(User currentUser, String problemId) {
        if (currentUser.getResolvedRole() != com.quertimizer.constant.UserRole.PROBLEM_GENERATOR) {
            return;
        }

        boolean hasPermission = problemGeneratorPermissionRepository.findAllByIdUserIdOrderByIdProblemIdAsc(currentUser.getUserId()).stream()
                .map(ProblemGeneratorPermission::getProblemId)
                .map(this::normalizePermissionKey)
                .anyMatch(problemId::equals);

        if (!hasPermission) {
            problemGeneratorPermissionRepository.save(ProblemGeneratorPermission.create(currentUser.getUserId(), problemId));
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

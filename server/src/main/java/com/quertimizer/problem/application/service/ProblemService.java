package com.quertimizer.problem.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.problem.application.input.ProblemCreateInput;
import com.quertimizer.problem.application.input.ProblemSqlDatasetInput;
import com.quertimizer.problem.application.input.ProblemSqlExecutionInput;
import com.quertimizer.problem.application.input.ProblemSqlReferenceInput;
import com.quertimizer.problem.application.output.AdminProblemOptionOutput;
import com.quertimizer.problem.application.output.ProblemCreateOutput;
import com.quertimizer.problem.application.output.ProblemDetailOutput;
import com.quertimizer.problem.application.output.ProblemListItemOutput;
import com.quertimizer.problem.application.output.ProblemPageOutput;
import com.quertimizer.problem.application.output.ProblemSetDetailOutput;
import com.quertimizer.problem.application.output.ProblemSetSummaryOutput;
import com.quertimizer.problem.application.output.ProblemSqlDatasetOutput;
import com.quertimizer.problem.application.output.ProblemSqlExecutionOutput;
import com.quertimizer.problem.application.output.ProblemSqlReferenceOutput;
import com.quertimizer.problem.application.output.ProblemSubmittedHistoryOutput;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.policy.ProblemManagementPolicy;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.application.port.ProblemGeneratorPermissionRepository;
import com.quertimizer.problem.application.port.ProblemRepository;
import com.quertimizer.problem.application.port.ProblemSetRepository;
import com.quertimizer.problem.application.port.ProblemSqlJudgePort;
import com.quertimizer.problem.application.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private final ProblemSqlJudgePort problemSqlJudgePort;
    private final ObjectMapper objectMapper;

    public ProblemPageOutput getProblems(int page,
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
        // 문제 목록 페이지를 조회
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

        List<ProblemListItemOutput> problems = problemPage.problems().stream()
                .map(problemEntry -> new ProblemListItemOutput(
                        problemEntry.problem().getProblemId(),
                        problemEntry.problem().getTitle(),
                        problemEntry.problem().getDescription(),
                        problemEntry.totalSubmitCount(),
                        problemEntry.successSubmitCount(),
                        problemEntry.spreadRate(),
                        problemEntry.submittedHistories().stream()
                                .map(this::toProblemSubmittedHistoryOutput)
                                .toList()
                ))
                .toList();

        return new ProblemPageOutput(
                problemPage.currentPage(),
                problemPage.pageSize(),
                problemPage.totalCount(),
                problemPage.totalPages(),
                problemPage.spreadRateMin(),
                problemPage.spreadRateMax(),
                problems
        );
    }

    public Optional<ProblemDetailOutput> getProblem(String problemId) {
        // 문제 상세를 조회
        return problemStore.findProblem(problemId)
                .map(problem -> problemStore.findProblemSet(problem.getResolvedProblemSetId())
                        .map(problemSet -> toProblemDetailOutput(problem, problemSet))
                        .orElseGet(() -> toProblemDetailOutput(problem)));
    }

    public List<ProblemSetSummaryOutput> getProblemSets(String authenticatedEmail) {
        // 문제 관리 가능한 테이블셋 목록을 조회
        User currentUser = requireProblemManagementUser(authenticatedEmail);

        if (currentUser.getResolvedRole() == UserRole.ADMIN) {
            return problemStore.findAllProblemSets().stream()
                    .map(ProblemSet::getProblemSetId)
                    .distinct()
                    .sorted()
                    .map(ProblemSetSummaryOutput::new)
                    .toList();
        }

        return findPermissionKeys(currentUser.getHandle()).stream()
                .map(permissionKey -> problemManagementPolicy.isScopedProblemSetId(permissionKey)
                        ? permissionKey
                        : problemManagementPolicy.isScopedProblemId(permissionKey) ? permissionKey.split("-")[0] : "")
                .filter(permissionKey -> !permissionKey.isBlank())
                .distinct()
                .sorted()
                .map(ProblemSetSummaryOutput::new)
                .toList();
    }

    public Optional<ProblemSetDetailOutput> getProblemSet(String problemSetId, String authenticatedEmail) {
        // 문제 테이블셋 상세를 조회
        User currentUser = requireProblemManagementUser(authenticatedEmail);
        String normalizedProblemSetId = normalizeProblemSetId(problemSetId);

        if (problemManagementPolicy.isScopedProblemSetId(normalizedProblemSetId)) {
            validateProblemSetAccess(currentUser, normalizedProblemSetId);
            return problemSetRepository.findById(normalizedProblemSetId)
                    .map(problemSet -> createScopedProblemSetDetail(problemSet));
        }

        ProblemSetGroup problemSetGroup = findProblemSetGroup(normalizedProblemSetId);
        if (problemSetGroup.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ProblemSetDetailOutput(
                normalizedProblemSetId,
                problemSetGroup.getDdl(DbmsType.POSTGRESQL),
                problemSetGroup.getDdl(DbmsType.MYSQL),
                problemSetGroup.getData(DbmsType.POSTGRESQL),
                problemSetGroup.getData(DbmsType.MYSQL)
        ));
    }

    public List<AdminProblemOptionOutput> getProblemOptions(String problemSetId, String authenticatedEmail) {
        // 문제 관리 가능한 문제 옵션 목록을 조회
        User currentUser = requireProblemManagementUser(authenticatedEmail);
        String scopedProblemSetId = normalizeScopedProblemSetId(problemSetId, null);
        validateProblemSetAccess(currentUser, scopedProblemSetId);

        if (currentUser.getResolvedRole() == UserRole.ADMIN) {
            return problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId).stream()
                    .map(problem -> new AdminProblemOptionOutput(problem.getProblemId()))
                    .toList();
        }

        Set<String> permissionKeys = findPermissionKeys(currentUser.getHandle());
        if (permissionKeys.contains(scopedProblemSetId)) {
            return problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId).stream()
                    .map(problem -> new AdminProblemOptionOutput(problem.getProblemId()))
                    .toList();
        }

        return problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId).stream()
                .filter(problem -> permissionKeys.contains(problem.getProblemId()))
                .map(problem -> new AdminProblemOptionOutput(problem.getProblemId()))
                .toList();
    }

    @Transactional
    public ProblemCreateOutput createProblem(ProblemCreateInput request, String authenticatedEmail) {
        // 문제 생성 또는 수정을 수행
        User currentUser = requireProblemManagementUser(authenticatedEmail);
        boolean useExistingProblemSet = "existing".equalsIgnoreCase(request.getProblemSetMode());
        boolean useExistingProblem = "existing".equalsIgnoreCase(request.getProblemMode());
        DbmsType dbmsType = resolveTargetDbmsType(request, useExistingProblemSet, useExistingProblem);
        String scopedProblemSetId = useExistingProblemSet
                ? normalizeScopedProblemSetId(request.getProblemSetId(), dbmsType)
                : createProblemSetId(dbmsType, createNextProblemSetBaseId());
        String scopedDdl = resolveScopedDdl(request, dbmsType);

        validateProblemWriteAccess(currentUser, useExistingProblemSet, useExistingProblem, scopedProblemSetId, request.getProblemId());

        ProblemSet existingProblemSet = useExistingProblemSet ? requireExistingProblemSet(scopedProblemSetId) : null;
        String canonicalDdl;
        String actualDataSql;
        if (useExistingProblemSet) {
            canonicalDdl = normalizeOptionalText(existingProblemSet.getDdl());
            actualDataSql = normalizeOptionalText(existingProblemSet.getActualDataSql());
        } else {
            canonicalDdl = resolveScopedDdl(request, dbmsType);
            actualDataSql = resolveScopedActualData(request, dbmsType);
        }

        String sampleDataSql = resolveScopedSampleData(request, dbmsType, useExistingProblemSet ? actualDataSql : "");
        String normalizedReferenceSql = requireText(request.getAnswerSql(), "기준 SQL이 필요하다.");
        ProblemSqlDatasetOutput sampleDataset = problemSqlJudgePort.createDataset(new ProblemSqlDatasetInput(dbmsType, scopedDdl, sampleDataSql, List.of()));
        ProblemSqlExecutionOutput sampleOutput = problemSqlJudgePort.execute(new ProblemSqlExecutionInput(sampleDataset.getDatasetId(), normalizedReferenceSql));
        ProblemSqlDatasetOutput judgeDataset = resolveProblemSetDataset(existingProblemSet, dbmsType, canonicalDdl, actualDataSql);
        ProblemSqlReferenceOutput reference = problemSqlJudgePort.createReference(new ProblemSqlReferenceInput(judgeDataset.getDatasetId(), normalizedReferenceSql));
        String templateVersion = createDatasetVersion(canonicalDdl, actualDataSql);

        if (useExistingProblemSet) {
            existingProblemSet.changeContent(canonicalDdl, actualDataSql, templateVersion, dbmsType, judgeDataset.getDatasetId());
        } else {
            createProblemSet(scopedProblemSetId, canonicalDdl, actualDataSql, templateVersion, dbmsType, judgeDataset.getDatasetId());
        }

        if (useExistingProblem) {
            return updateProblem(
                    request, scopedProblemSetId, scopedDdl, dbmsType,
                    sampleDataSql, sampleOutput, sampleDataset.getDatasetId(), reference
            );
        }

        String baseProblemSetId = extractBaseProblemSetId(scopedProblemSetId);
        String problemId = createProblemId(dbmsType, baseProblemSetId, createNextProblemSequence(baseProblemSetId));

        problemRepository.save(Problem.create(
                problemId,
                scopedProblemSetId,
                request.getTitle().trim(),
                request.getDescription().trim(),
                scopedDdl,
                dbmsType,
                request.getCondition().trim(),
                request.getOutput().trim(),
                sampleDataSql,
                serializeSampleOutput(sampleOutput),
                reference.getResultHash(),
                "",
                sampleDataset.getDatasetId(),
                reference.getReferenceId()
        ));

        addCreatedProblemPermissionIfNeeded(currentUser, problemId);
        problemStore.loadProblems();
        return new ProblemCreateOutput(problemId);
    }

    private DbmsType resolveDbmsType(String dbms) {
        // 요청 DBMS 값을 내부 유형으로 맞춘다
        return DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL);
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

    private ProblemSetDetailOutput createScopedProblemSetDetail(ProblemSet problemSet) {
        // 스코프 문제 테이블셋 상세 생성
        if (problemSet.getDbmsType() == DbmsType.MYSQL) {
            return new ProblemSetDetailOutput(
                    problemSet.getProblemSetId(),
                    "",
                    normalizeOptionalText(problemSet.getDdl()),
                    "",
                    normalizeOptionalText(problemSet.getData())
            );
        }

        return new ProblemSetDetailOutput(
                problemSet.getProblemSetId(),
                normalizeOptionalText(problemSet.getDdl()),
                "",
                normalizeOptionalText(problemSet.getData()),
                ""
        );
    }

    private ProblemSet requireExistingProblemSet(String problemSetId) {
        // 기존 문제 테이블셋 필수값 검증
        return problemSetRepository.findById(problemSetId)
                .orElseThrow(() -> new BusinessException(PROBLEM_SET_NOT_FOUND.getMessage(), HttpStatus.BAD_REQUEST));
    }

    private ProblemSqlDatasetOutput resolveProblemSetDataset(ProblemSet existingProblemSet,
                                                             DbmsType dbmsType,
                                                             String canonicalDdl,
                                                             String actualDataSql) {
        // 문제 테이블셋의 sql-judge 데이터셋 키를 준비
        if (!canonicalDdl.isBlank() && !actualDataSql.isBlank()) {
            return problemSqlJudgePort.createDataset(new ProblemSqlDatasetInput(dbmsType, canonicalDdl, actualDataSql, List.of()));
        }

        if (existingProblemSet != null && existingProblemSet.getJudgeDatasetId() != null && !existingProblemSet.getJudgeDatasetId().isBlank()) {
            return new ProblemSqlDatasetOutput(existingProblemSet.getJudgeDatasetId());
        }

        return problemSqlJudgePort.createDataset(new ProblemSqlDatasetInput(dbmsType, canonicalDdl, actualDataSql, List.of()));
    }

    private ProblemSet createProblemSet(String problemSetId,
                                        String canonicalDdl,
                                        String actualDataSql,
                                        String templateVersion,
                                        DbmsType dbmsType,
                                        String judgeDatasetId) {
        // 문제 테이블셋 생성
        return problemSetRepository.save(ProblemSet.create(
                problemSetId,
                canonicalDdl,
                actualDataSql,
                templateVersion,
                dbmsType,
                judgeDatasetId
        ));
    }

    private ProblemCreateOutput updateProblem(ProblemCreateInput request,
                                              String problemSetId,
                                              String scopedDdl,
                                              DbmsType dbmsType,
                                              String sampleDataSql,
                                              ProblemSqlExecutionOutput sampleOutput,
                                              String sampleDatasetId,
                                              ProblemSqlReferenceOutput reference) {
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
                dbmsType,
                request.getCondition().trim(),
                request.getOutput().trim(),
                sampleDataSql,
                serializeSampleOutput(sampleOutput),
                reference.getResultHash(),
                "",
                sampleDatasetId,
                reference.getReferenceId()
        );

        problemStore.loadProblems();
        return new ProblemCreateOutput(problem.getProblemId());
    }

    private ProblemSubmittedHistoryOutput toProblemSubmittedHistoryOutput(com.quertimizer.problem.domain.entity.ProblemSolveHistory history) {
        // 문제 제출 기록 응답으로 변환
        DbmsType dbmsType = history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;

        return new ProblemSubmittedHistoryOutput(
                dbmsType.getValue(),
                history.getHandle(),
                com.quertimizer.global.constant.ExecutionPlanElementIndexes.normalize(dbmsType, history.getExecutionPlanElement()),
                history.getExecutionTimeMs(),
                history.getCost()
        );
    }

    private ProblemDetailOutput toProblemDetailOutput(Problem problem, ProblemSet problemSet) {
        // 문제 상세 응답으로 변환
        return new ProblemDetailOutput(
                problem.getProblemId(),
                problem.getTitle(),
                normalizeOptionalText(problem.getDescription()),
                problem.getDbmsType() == DbmsType.POSTGRESQL ? normalizeOptionalText(problem.getDdl()) : "",
                problem.getDbmsType() == DbmsType.MYSQL ? normalizeOptionalText(problem.getDdl()) : "",
                problemSet.getDbmsType() == DbmsType.POSTGRESQL ? normalizeOptionalText(problemSet.getData()) : "",
                problemSet.getDbmsType() == DbmsType.MYSQL ? normalizeOptionalText(problemSet.getData()) : "",
                normalizeOptionalText(problem.getCondition()),
                normalizeOptionalText(problem.getOutput()),
                normalizeOptionalText(problem.getOutputSample()),
                normalizeOptionalText(problem.getSampleDataSql()),
                normalizeAnswerSql(problem),
                normalizeOptionalText(problem.getAnswer()),
                problem.getDbmsType().getValue()
        );
    }

    private ProblemDetailOutput toProblemDetailOutput(Problem problem) {
        // 문제 상세 응답으로 변환
        return new ProblemDetailOutput(
                problem.getProblemId(),
                problem.getTitle(),
                normalizeOptionalText(problem.getDescription()),
                problem.getDbmsType() == DbmsType.POSTGRESQL ? normalizeOptionalText(problem.getDdl()) : "",
                problem.getDbmsType() == DbmsType.MYSQL ? normalizeOptionalText(problem.getDdl()) : "",
                "",
                "",
                normalizeOptionalText(problem.getCondition()),
                normalizeOptionalText(problem.getOutput()),
                normalizeOptionalText(problem.getOutputSample()),
                normalizeOptionalText(problem.getSampleDataSql()),
                normalizeAnswerSql(problem),
                normalizeOptionalText(problem.getAnswer()),
                problem.getDbmsType().getValue()
        );
    }

    private String normalizeAnswerSql(Problem problem) {
        // 정답 SQL 정규화
        if (problem.getAnswerSql() != null && !problem.getAnswerSql().isBlank()) {
            return problem.getAnswerSql();
        }

        String legacyAnswer = normalizeOptionalText(problem.getAnswer());
        if (legacyAnswer.toUpperCase().startsWith("SELECT ") || legacyAnswer.toUpperCase().startsWith("WITH ")) {
            return legacyAnswer;
        }

        return "";
    }

    private String resolveScopedDdl(ProblemCreateInput request, DbmsType dbmsType) {
        // 스코프 DDL 결정
        if (dbmsType == DbmsType.MYSQL) {
            return requireText(request.getDdlMysql(), "MySQL DDL이 필요하다.");
        }

        return requireText(request.getDdlPostgresql(), "PostgreSQL DDL이 필요하다.");
    }

    private String resolveScopedActualData(ProblemCreateInput request, DbmsType dbmsType) {
        // 스코프 실제 채점 데이터 SQL 결정
        if (dbmsType == DbmsType.MYSQL) {
            return requireText(request.getActualDataMysql(), "MySQL 실제 채점 데이터 SQL이 필요하다.");
        }

        return requireText(request.getActualDataPostgresql(), "PostgreSQL 실제 채점 데이터 SQL이 필요하다.");
    }

    private String resolveScopedSampleData(ProblemCreateInput request, DbmsType dbmsType, String fallbackDataSql) {
        // 스코프 예시 데이터 SQL 결정
        if (dbmsType == DbmsType.MYSQL) {
            String sampleDataSql = normalizeOptionalText(request.getSampleDataMysql());
            return !sampleDataSql.isBlank() ? sampleDataSql : requireText(fallbackDataSql, "MySQL 예시 데이터 SQL이 필요하다.");
        }

        String sampleDataSql = normalizeOptionalText(request.getSampleDataPostgresql());
        return !sampleDataSql.isBlank() ? sampleDataSql : requireText(fallbackDataSql, "PostgreSQL 예시 데이터 SQL이 필요하다.");
    }

    private ProblemSetGroup findProblemSetGroup(String problemSetId) {
        // DBMS별 문제 테이블셋 조회
        Map<DbmsType, ProblemSet> problemSetsByDbms = new EnumMap<>(DbmsType.class);
        for (DbmsType dbmsType : DbmsType.values()) {
            problemSetRepository.findById(createProblemSetId(dbmsType, problemSetId))
                    .ifPresent(problemSet -> problemSetsByDbms.put(dbmsType, problemSet));
        }

        return new ProblemSetGroup(problemSetsByDbms);
    }

    private String createNextProblemSetBaseId() {
        // 다음 문제 테이블셋 기준 번호 생성
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
        // 다음 문제 Sequence 생성
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
        // 문제 테이블셋 번호 생성
        return dbmsType.getIdPrefix() + normalizeBaseProblemSetId(baseProblemSetId);
    }

    private String createProblemId(DbmsType dbmsType, String baseProblemSetId, int sequence) {
        // 문제 번호 생성
        return createProblemSetId(dbmsType, baseProblemSetId) + "-" + formatFiveDigits(sequence);
    }

    private String normalizeProblemSetId(String problemSetId) {
        // 문제 테이블셋 번호 정규화
        return Optional.ofNullable(problemSetId)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new BusinessException(PROBLEM_SET_ID_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST));
    }

    private String normalizeScopedProblemSetId(String problemSetId, DbmsType dbmsType) {
        // 스코프 문제 테이블셋 번호 정규화
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
        // 스코프 DBMS 유형 결정
        return DbmsType.fromScopedId(scopedId).orElse(DbmsType.POSTGRESQL);
    }

    private String extractBaseProblemSetId(String scopedValue) {
        // 기준 문제 테이블셋 번호 추출
        if (scopedValue == null || scopedValue.isBlank()) {
            return "";
        }

        String[] tokens = scopedValue.split("-");
        String scopedProblemSetId = tokens.length > 0 ? tokens[0] : scopedValue;
        return DbmsType.extractBaseProblemSetId(scopedProblemSetId);
    }

    private String normalizeBaseProblemSetId(String problemSetId) {
        // 기준 문제 테이블셋 번호 정규화
        String normalizedProblemSetId = normalizeProblemSetId(problemSetId);
        return DbmsType.extractBaseProblemSetId(normalizedProblemSetId);
    }

    private String formatFiveDigits(int value) {
        // Five Digits 포맷
        return "%05d".formatted(value);
    }

    private User requireProblemManagementUser(String authenticatedEmail) {
        // 문제 관리 사용자 필수값 검증
        User user = authService.findAuthenticatedUser(authenticatedEmail)
                .orElseThrow(() -> new BusinessException(PROBLEM_MANAGEMENT_ACCESS_DENIED.getMessage(), HttpStatus.FORBIDDEN));

        problemManagementPolicy.validateProblemManagementUser(user);
        return user;
    }

    private void validateProblemSetAccess(User currentUser, String scopedProblemSetId) {
        // 문제 테이블셋 접근 검증
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
        // 권한 키 목록 조회
        return problemGeneratorPermissionRepository.findAllByIdHandleOrderByIdProblemIdAsc(handle).stream()
                .map(ProblemGeneratorPermission::getProblemId)
                .map(problemManagementPolicy::normalizePermissionKey)
                .filter(permissionKey -> !permissionKey.isBlank())
                .collect(Collectors.toSet());
    }

    private void addCreatedProblemPermissionIfNeeded(User currentUser, String problemId) {
        // Created 문제 권한 If Needed 추가
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
        // 텍스트 필수값 검증
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }

        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        // Optional 텍스트 정규화
        return value != null ? value.trim() : "";
    }

    private String createDatasetVersion(String canonicalDdl, String actualDataSql) {
        // 데이터셋 버전 해시 생성
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(normalizeOptionalText(canonicalDdl).getBytes(StandardCharsets.UTF_8));
            messageDigest.update((byte) 0);
            messageDigest.update(normalizeOptionalText(actualDataSql).getBytes(StandardCharsets.UTF_8));
            return toHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("데이터셋 버전 해시를 생성할 수 없다.", exception);
        }
    }

    private String toHex(byte[] bytes) {
        // 바이트 배열을 hex 문자열로 변환
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append("%02x".formatted(value & 0xff));
        }

        return builder.toString();
    }

    private String serializeSampleOutput(ProblemSqlExecutionOutput sampleOutput) {
        // sample output 구조를 JSON 문자열로 저장
        try {
            return objectMapper.writeValueAsString(sampleOutput);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("sample output 직렬화에 실패했다.", exception);
        }
    }

    private static final class ProblemSetGroup {
        // DBMS별 문제 테이블셋 묶음 처리

        private final Map<DbmsType, ProblemSet> problemSetsByDbms;

        private ProblemSetGroup(Map<DbmsType, ProblemSet> problemSetsByDbms) {
            this.problemSetsByDbms = problemSetsByDbms;
        }

        private boolean isEmpty() {
            // Empty 여부 확인
            return problemSetsByDbms.isEmpty();
        }

        private String getDdl(DbmsType dbmsType) {
            // DBMS별 DDL 조회
            ProblemSet problemSet = problemSetsByDbms.get(dbmsType);
            return problemSet != null && problemSet.getDdl() != null ? problemSet.getDdl().trim() : "";
        }

        private String getData(DbmsType dbmsType) {
            // DBMS별 데이터 SQL 조회
            ProblemSet problemSet = problemSetsByDbms.get(dbmsType);
            return problemSet != null && problemSet.getData() != null ? problemSet.getData().trim() : "";
        }
    }
}

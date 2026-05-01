package com.quertimizer.problem.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.judge.application.input.CreateJudgeDatasetInput;
import com.quertimizer.judge.application.input.CreateJudgeReferenceInput;
import com.quertimizer.judge.application.input.ExecuteIsolatedJudgeSqlInput;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.output.SqlReferenceResult;
import com.quertimizer.judge.application.port.JudgePort;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import com.quertimizer.judge.domain.model.IsolationPolicy;
import com.quertimizer.problem.application.input.CreateProblemInput;
import com.quertimizer.problem.application.input.ProblemCreateInput;
import com.quertimizer.problem.application.output.ProblemCreateOutput;
import com.quertimizer.problem.application.output.ProblemSqlDatasetOutput;
import com.quertimizer.problem.application.output.ProblemSqlExecutionOutput;
import com.quertimizer.problem.application.output.ProblemSqlReferenceOutput;
import com.quertimizer.problem.application.port.ProblemGeneratorPermissionRepository;
import com.quertimizer.problem.application.port.ProblemRepository;
import com.quertimizer.problem.application.port.ProblemSetRepository;
import com.quertimizer.problem.application.service.ProblemService;
import com.quertimizer.problem.application.store.ProblemStore;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.policy.ProblemManagementPolicy;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.EXISTING_PROBLEM_ID_REQUIRED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_NOT_FOUND;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_NOT_IN_PROBLEM_SET;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_SET_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class CreateProblem {

    private final ProblemStore problemStore;
    private final ProblemRepository problemRepository;
    private final ProblemSetRepository problemSetRepository;
    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;
    private final JudgePort judgePort;
    private final ProblemService problemService;
    private final ProblemManagementPolicy problemManagementPolicy;
    private final ObjectMapper objectMapper;

    /**
     * 관리자 문제를 생성한다.
     *
     * <ol>
     *   <li>권한과 생성 범위 결정
     *   <li>문제셋 기준 SQL 자료 결정
     *   <li>예시 데이터셋 실행
     *   <li>채점 데이터셋과 기준 SQL 등록
     *   <li>문제셋 메타데이터 저장
     *   <li>문제 생성 또는 수정
     * </ol>
     *
     * @param input 문제 생성 요청과 인증 이메일 입력
     */
    @Transactional
    public ProblemCreateOutput execute(CreateProblemInput input) {
        ProblemCreateInput request = input.getProblem();
        User currentUser = problemService.requireProblemManagementUser(input.getAuthenticatedEmail());
        boolean useExistingProblemSet = request.isExistingProblemSet();
        boolean useExistingProblem = request.isExistingProblem();
        DbmsType dbmsType = resolveTargetDbmsType(request, useExistingProblemSet, useExistingProblem);
        String scopedProblemSetId = useExistingProblemSet
                ? problemService.normalizeScopedProblemSetId(request.getProblemSetId(), dbmsType)
                : problemService.createProblemSetId(dbmsType, createNextProblemSetBaseId());
        String scopedDdl = resolveScopedDdl(request, dbmsType);
        Set<String> permissionKeys = problemService.findPermissionKeys(currentUser.getHandle());
        problemManagementPolicy.validateProblemWriteAccess(
                currentUser,
                permissionKeys,
                useExistingProblemSet,
                useExistingProblem,
                scopedProblemSetId,
                normalizeProblemIdForWrite(request.getProblemId())
        );

        ProblemSet existingProblemSet = useExistingProblemSet ? requireExistingProblemSet(scopedProblemSetId) : null;
        String canonicalDdl = useExistingProblemSet
                ? problemService.normalizeOptionalText(existingProblemSet.getDdl())
                : scopedDdl;
        String actualDataSql = useExistingProblemSet
                ? problemService.normalizeOptionalText(existingProblemSet.getActualDataSql())
                : resolveScopedActualData(request, dbmsType);

        String sampleDataSql = resolveScopedSampleData(request, dbmsType, useExistingProblemSet ? actualDataSql : "");
        String normalizedReferenceSql = problemService.requireText(request.getAnswerSql(), "기준 SQL이 필요하다.");
        ProblemSqlDatasetOutput sampleDataset = createJudgeDataset(dbmsType, scopedDdl, sampleDataSql);
        ProblemSqlExecutionOutput sampleOutput = executeJudgeSql(sampleDataset.getDatasetId(), normalizedReferenceSql);

        ProblemSqlDatasetOutput judgeDataset = resolveProblemSetDataset(existingProblemSet, dbmsType, canonicalDdl, actualDataSql);
        ProblemSqlReferenceOutput reference = createJudgeReference(judgeDataset.getDatasetId(), normalizedReferenceSql);
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

        String baseProblemSetId = problemService.extractBaseProblemSetId(scopedProblemSetId);
        String problemId = createProblemId(dbmsType, baseProblemSetId, createNextProblemSequence(baseProblemSetId));

        problemRepository.save(Problem.create(
                problemId, scopedProblemSetId,
                request.getTitle().trim(), request.getDescription().trim(),
                scopedDdl, dbmsType,
                request.getCondition().trim(), request.getOutput().trim(),
                sampleDataSql, serializeSampleOutput(sampleOutput),
                reference.getResultHash(), "",
                sampleDataset.getDatasetId(), reference.getReferenceId()
        ));

        addCreatedProblemPermissionIfNeeded(currentUser, problemId);
        problemStore.loadProblems();
        return new ProblemCreateOutput(problemId);
    }

    private DbmsType resolveTargetDbmsType(ProblemCreateInput request,
                                           boolean useExistingProblemSet, boolean useExistingProblem) {
        // 기존 문제 수정 시 문제 번호 스코프 기준 DBMS 유형 결정
        if (useExistingProblem && request.getProblemId() != null && !request.getProblemId().isBlank()) {
            return problemService.resolveScopedDbmsType(request.getProblemId());
        }

        // 기존 문제셋 사용 시 문제셋 번호 스코프 기준 DBMS 유형 결정
        if (useExistingProblemSet && request.getProblemSetId() != null && !request.getProblemSetId().isBlank()) {
            return problemService.resolveScopedDbmsType(request.getProblemSetId());
        }

        // 신규 문제셋 생성 시 요청 DBMS 유형 기본값 적용
        return problemService.resolveDbmsType(request.getDbms());
    }

    private String resolveScopedDdl(ProblemCreateInput request, DbmsType dbmsType) {
        // MySQL 요청 시 MySQL DDL 필수값 반환
        if (dbmsType == DbmsType.MYSQL) {
            return problemService.requireText(request.getDdlMysql(), "MySQL DDL이 필요하다.");
        }

        // PostgreSQL 요청 시 PostgreSQL DDL 필수값 반환
        return problemService.requireText(request.getDdlPostgresql(), "PostgreSQL DDL이 필요하다.");
    }

    private String resolveScopedActualData(ProblemCreateInput request, DbmsType dbmsType) {
        // MySQL 요청 시 MySQL 실제 채점 데이터 SQL 필수값 반환
        if (dbmsType == DbmsType.MYSQL) {
            return problemService.requireText(request.getActualDataMysql(), "MySQL 실제 채점 데이터 SQL이 필요하다.");
        }

        // PostgreSQL 요청 시 PostgreSQL 실제 채점 데이터 SQL 필수값 반환
        return problemService.requireText(request.getActualDataPostgresql(), "PostgreSQL 실제 채점 데이터 SQL이 필요하다.");
    }

    private String resolveScopedSampleData(ProblemCreateInput request, DbmsType dbmsType, String fallbackDataSql) {
        // MySQL 요청 시 예시 데이터 SQL 또는 fallback 데이터 SQL 반환
        if (dbmsType == DbmsType.MYSQL) {
            String sampleDataSql = problemService.normalizeOptionalText(request.getSampleDataMysql());
            return !sampleDataSql.isBlank()
                    ? sampleDataSql
                    : problemService.requireText(fallbackDataSql, "MySQL 예시 데이터 SQL이 필요하다.");
        }

        // PostgreSQL 요청 시 예시 데이터 SQL 또는 fallback 데이터 SQL 반환
        String sampleDataSql = problemService.normalizeOptionalText(request.getSampleDataPostgresql());
        return !sampleDataSql.isBlank()
                ? sampleDataSql
                : problemService.requireText(fallbackDataSql, "PostgreSQL 예시 데이터 SQL이 필요하다.");
    }

    private String normalizeProblemIdForWrite(String problemId) {
        // 기존 문제 수정용 문제 번호 정규화
        return problemId == null || problemId.isBlank()
                ? ""
                : problemService.requireText(problemId, EXISTING_PROBLEM_ID_REQUIRED.getMessage());
    }

    private ProblemSet requireExistingProblemSet(String problemSetId) {
        // 기존 문제 테이블셋 조회와 누락 검증
        return problemSetRepository.findById(problemSetId)
                .orElseThrow(() -> new BusinessException(PROBLEM_SET_NOT_FOUND.getMessage(), HttpStatus.BAD_REQUEST));
    }

    private ProblemSqlDatasetOutput resolveProblemSetDataset(ProblemSet existingProblemSet,
                                                             DbmsType dbmsType,
                                                             String canonicalDdl,
                                                             String actualDataSql) {
        // 원본 SQL이 있는 문제셋의 judge 데이터셋 재등록
        if (!canonicalDdl.isBlank() && !actualDataSql.isBlank()) {
            return createJudgeDataset(dbmsType, canonicalDdl, actualDataSql);
        }

        // 원본 SQL이 없는 기존 문제셋의 저장된 데이터셋 키 재사용
        if (existingProblemSet != null && existingProblemSet.getJudgeDatasetId() != null
                && !existingProblemSet.getJudgeDatasetId().isBlank()) {
            return new ProblemSqlDatasetOutput(existingProblemSet.getJudgeDatasetId());
        }

        // 원본 SQL과 저장 키가 모두 부족한 경우 judge 검증 경로로 위임
        return createJudgeDataset(dbmsType, canonicalDdl, actualDataSql);
    }

    private ProblemSqlDatasetOutput createJudgeDataset(DbmsType dbmsType, String ddl, String dataSql) {
        // 문제 SQL 자료를 judge 데이터셋 생성 입력으로 변환 후 키 반환
        JudgeDatasetId datasetId = judgePort.createDataset(new CreateJudgeDatasetInput(
                toJudgeDbmsType(dbmsType), ddl, dataSql, List.of()
        ));
        return new ProblemSqlDatasetOutput(datasetId.getValue());
    }

    private ProblemSqlExecutionOutput executeJudgeSql(String datasetId, String sql) {
        // 문제 생성 검증용 격리 실행 후 화면 저장용 결과로 변환
        SqlExecutionResult result = judgePort.executeIsolated(new ExecuteIsolatedJudgeSqlInput(
                new JudgeExecutionId("problem-create-" + UUID.randomUUID()),
                new JudgeDatasetId(datasetId),
                List.of(),
                sql,
                IsolationPolicy.cleanRoom(),
                ExecutionOptions.officialCost()
        ));
        return new ProblemSqlExecutionOutput(result.getColumns(), result.getRows(), result.getRowCount());
    }

    private ProblemSqlReferenceOutput createJudgeReference(String datasetId, String referenceSql) {
        // judge 기준 SQL 생성 결과에서 problem 저장용 키와 해시 추출
        SqlReferenceResult result = judgePort.createReference(new CreateJudgeReferenceInput(
                new JudgeDatasetId(datasetId), referenceSql, ExecutionOptions.officialCost()
        ));
        return new ProblemSqlReferenceOutput(result.getReferenceId().getValue(), result.getResultHash());
    }

    private com.quertimizer.judge.domain.model.DbmsType toJudgeDbmsType(DbmsType dbmsType) {
        // problem DBMS 유형을 judge DBMS 유형으로 변환
        return switch (dbmsType) {
            case POSTGRESQL -> com.quertimizer.judge.domain.model.DbmsType.POSTGRESQL;
            case MYSQL -> com.quertimizer.judge.domain.model.DbmsType.MYSQL;
        };
    }

    private void createProblemSet(String problemSetId, String canonicalDdl, String actualDataSql,
                                  String templateVersion, DbmsType dbmsType, String judgeDatasetId) {
        // 문제 테이블셋 엔티티 생성과 저장
        problemSetRepository.save(ProblemSet.create(
                problemSetId,
                canonicalDdl,
                actualDataSql,
                templateVersion,
                dbmsType,
                judgeDatasetId
        ));
    }

    private ProblemCreateOutput updateProblem(ProblemCreateInput request, String problemSetId,
                                              String scopedDdl, DbmsType dbmsType,
                                              String sampleDataSql, ProblemSqlExecutionOutput sampleOutput,
                                              String sampleDatasetId, ProblemSqlReferenceOutput reference) {
        // 수정 대상 문제 조회와 문제셋 일치 검증
        String problemId = problemService.requireText(request.getProblemId(), EXISTING_PROBLEM_ID_REQUIRED.getMessage());
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(PROBLEM_NOT_FOUND.getMessage(), HttpStatus.BAD_REQUEST));

        // 수정 대상 문제와 요청 문제셋 일치 여부 검사
        if (!problem.getResolvedProblemSetId().equals(problemSetId)) {
            throw new BusinessException(PROBLEM_NOT_IN_PROBLEM_SET.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // 문제 내용과 judge 키 갱신
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

    private String createNextProblemSetBaseId() {
        // 다음 문제 테이블셋 기준 번호 생성
        return problemSetRepository.findAll().stream()
                .map(ProblemSet::getBaseProblemSetId)
                .filter(problemSetId -> !problemSetId.isBlank())
                .map(this::parseProblemSetIdNumber)
                .max(Comparator.naturalOrder())
                .map(maxValue -> formatFiveDigits(maxValue + 1))
                .orElse("00001");
    }

    private int createNextProblemSequence(String baseProblemSetId) {
        // 다음 문제 순번 생성
        return problemRepository.findAll().stream()
                .map(Problem::getProblemId)
                .filter(problemId -> problemService.extractBaseProblemSetId(problemId).equals(baseProblemSetId))
                .map(problemId -> problemId.split("-"))
                .filter(tokens -> tokens.length == 2)
                .map(tokens -> parseProblemSequence(tokens[1]))
                .max(Comparator.naturalOrder())
                .map(maxValue -> maxValue + 1)
                .orElse(1);
    }

    private int parseProblemSetIdNumber(String problemSetId) {
        // 문제 테이블셋 번호 숫자 변환
        try {
            return Integer.parseInt(problemSetId);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int parseProblemSequence(String sequence) {
        // 문제 순번 숫자 변환
        try {
            return Integer.parseInt(sequence);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String createProblemId(DbmsType dbmsType, String baseProblemSetId, int sequence) {
        // DBMS prefix와 순번 조합 문제 번호 생성
        return problemService.createProblemSetId(dbmsType, baseProblemSetId) + "-" + formatFiveDigits(sequence);
    }

    private String formatFiveDigits(int value) {
        // 다섯 자리 문자열 포맷
        return "%05d".formatted(value);
    }

    private void addCreatedProblemPermissionIfNeeded(User currentUser, String problemId) {
        // 문제 생성자 권한 추가 대상 여부 검사
        if (currentUser.getResolvedRole() != UserRole.PROBLEM_GENERATOR) {
            return;
        }

        // 기존 문제 권한 보유 여부 검사
        boolean hasPermission = problemGeneratorPermissionRepository.findAllByIdHandleOrderByIdProblemIdAsc(currentUser.getHandle()).stream()
                .map(ProblemGeneratorPermission::getProblemId)
                .map(problemManagementPolicy::normalizePermissionKey)
                .anyMatch(problemId::equals);

        // 신규 문제 권한 미보유 시 권한 저장
        if (!hasPermission) {
            problemGeneratorPermissionRepository.save(ProblemGeneratorPermission.create(currentUser.getHandle(), problemId));
        }
    }

    private String createDatasetVersion(String canonicalDdl, String actualDataSql) {
        // DDL과 데이터 SQL 기준 데이터셋 버전 해시 생성
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(problemService.normalizeOptionalText(canonicalDdl).getBytes(StandardCharsets.UTF_8));
            messageDigest.update((byte) 0);
            messageDigest.update(problemService.normalizeOptionalText(actualDataSql).getBytes(StandardCharsets.UTF_8));
            return toHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("데이터셋 버전 해시를 생성할 수 없다.", exception);
        }
    }

    private String toHex(byte[] bytes) {
        // 바이트 배열을 16진수 문자열로 변환
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append("%02x".formatted(value & 0xff));
        }

        // 완성된 16진수 문자열 반환
        return builder.toString();
    }

    private String serializeSampleOutput(ProblemSqlExecutionOutput sampleOutput) {
        // 예시 출력 구조 JSON 문자열 변환
        try {
            return objectMapper.writeValueAsString(sampleOutput);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("예시 출력 직렬화에 실패했다.", exception);
        }
    }
}

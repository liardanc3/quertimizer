package com.quertimizer.service;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.endpoint.api.dto.request.ProblemCreateReq;
import com.quertimizer.endpoint.api.dto.response.ProblemCreateRes;
import com.quertimizer.endpoint.api.dto.response.ProblemDetailRes;
import com.quertimizer.endpoint.api.dto.response.ProblemListItemRes;
import com.quertimizer.endpoint.api.dto.response.ProblemPageRes;
import com.quertimizer.endpoint.api.dto.response.ProblemSetDetailRes;
import com.quertimizer.endpoint.api.dto.response.ProblemSetSummaryRes;
import com.quertimizer.endpoint.api.dto.response.ProblemSubmittedHistoryRes;
import com.quertimizer.entity.Problem;
import com.quertimizer.entity.ProblemSet;
import com.quertimizer.exception.BusinessException;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemStore problemStore;
    private final ProblemRepository problemRepository;
    private final ProblemSetRepository problemSetRepository;

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

    public List<ProblemSetSummaryRes> getProblemSets() {
        return problemStore.findAllProblemSets().stream()
                .map(ProblemSet::getBaseProblemSetId)
                .distinct()
                .sorted()
                .map(ProblemSetSummaryRes::new)
                .toList();
    }

    public Optional<ProblemSetDetailRes> getProblemSet(String problemSetId) {
        String baseProblemSetId = normalizeBaseProblemSetId(problemSetId);
        ProblemSetPair problemSetPair = findProblemSetPair(baseProblemSetId);

        if (problemSetPair.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(ProblemSetDetailRes.from(baseProblemSetId, problemSetPair.postgresqlProblemSet(), problemSetPair.oracleProblemSet()));
    }

    @Transactional
    public ProblemCreateRes createProblem(ProblemCreateReq request) {
        boolean useExistingProblemSet = "existing".equalsIgnoreCase(request.getProblemSetMode());
        String baseProblemSetId = useExistingProblemSet
                ? normalizeBaseProblemSetId(request.getProblemSetId())
                : createNextProblemSetBaseId();

        ProblemSetPair problemSetPair = useExistingProblemSet
                ? requireExistingProblemSetPair(baseProblemSetId)
                : createProblemSetPair(baseProblemSetId, request);

        String postgresDdl = requireText(request.getDdlPostgresql(), "PostgreSQL DDL이 필요하다.");
        String oracleDdl = requireText(request.getDdlOracle(), "Oracle DDL이 필요하다.");
        int nextProblemSequence = createNextProblemSequence(baseProblemSetId);
        String postgresProblemId = createProblemId(DbmsType.POSTGRESQL, baseProblemSetId, nextProblemSequence);
        String oracleProblemId = createProblemId(DbmsType.ORACLE, baseProblemSetId, nextProblemSequence);

        problemRepository.save(Problem.create(
                postgresProblemId,
                problemSetPair.postgresqlProblemSet().getProblemSetId(),
                request.getTitle().trim(),
                request.getDescription().trim(),
                postgresDdl,
                true,
                false,
                request.getCondition().trim(),
                request.getOutput().trim(),
                normalizeOptionalText(request.getOutputSample()),
                normalizeOptionalText(request.getAnswer())
        ));
        problemRepository.save(Problem.create(
                oracleProblemId,
                problemSetPair.oracleProblemSet().getProblemSetId(),
                request.getTitle().trim(),
                request.getDescription().trim(),
                oracleDdl,
                false,
                true,
                request.getCondition().trim(),
                request.getOutput().trim(),
                normalizeOptionalText(request.getOutputSample()),
                normalizeOptionalText(request.getAnswer())
        ));

        problemStore.loadProblems();

        return new ProblemCreateRes(postgresProblemId);
    }

    private DbmsType resolveDbmsType(String dbms) {
        return "oracle".equalsIgnoreCase(dbms) ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
    }

    private ProblemSetPair requireExistingProblemSetPair(String problemSetId) {
        ProblemSetPair problemSetPair = findProblemSetPair(problemSetId);
        if (problemSetPair.postgresqlProblemSet() == null || problemSetPair.oracleProblemSet() == null) {
            throw new BusinessException("PostgreSQL과 Oracle 테이블셋이 모두 필요하다.", HttpStatus.BAD_REQUEST);
        }

        return problemSetPair;
    }

    private ProblemSetPair createProblemSetPair(String baseProblemSetId, ProblemCreateReq request) {
        String ddlPostgresql = requireText(request.getDdlPostgresql(), "PostgreSQL DDL이 필요하다.");
        String ddlOracle = requireText(request.getDdlOracle(), "Oracle DDL이 필요하다.");
        String dataPostgresql = requireText(request.getDataPostgresql(), "PostgreSQL 데이터 SQL이 필요하다.");
        String dataOracle = requireText(request.getDataOracle(), "Oracle 데이터 SQL이 필요하다.");

        ProblemSet postgresqlProblemSet = problemSetRepository.save(ProblemSet.create(
                createProblemSetId(DbmsType.POSTGRESQL, baseProblemSetId),
                ddlPostgresql,
                dataPostgresql,
                true,
                false
        ));
        ProblemSet oracleProblemSet = problemSetRepository.save(ProblemSet.create(
                createProblemSetId(DbmsType.ORACLE, baseProblemSetId),
                ddlOracle,
                dataOracle,
                false,
                true
        ));

        return new ProblemSetPair(postgresqlProblemSet, oracleProblemSet);
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

    private String normalizeBaseProblemSetId(String problemSetId) {
        if (problemSetId == null || problemSetId.isBlank()) {
            throw new BusinessException("기존 테이블셋 번호가 필요하다.", HttpStatus.BAD_REQUEST);
        }

        String normalizedProblemSetId = problemSetId.trim();
        if (normalizedProblemSetId.matches("^[PO]\\d{5}$")) {
            return normalizedProblemSetId.substring(1);
        }

        return normalizedProblemSetId;
    }

    private String extractBaseProblemSetId(String problemId) {
        if (problemId == null || problemId.isBlank()) {
            return "";
        }

        String[] tokens = problemId.split("-");
        String scopedProblemSetId = tokens.length > 0 ? tokens[0] : problemId;
        return scopedProblemSetId.matches("^[PO]\\d{5}$") ? scopedProblemSetId.substring(1) : scopedProblemSetId;
    }

    private String formatFiveDigits(int value) {
        return "%05d".formatted(value);
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

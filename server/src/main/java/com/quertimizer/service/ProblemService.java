package com.quertimizer.service;

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
                                      String solveState,
                                      String currentUserId,
                                      String solvedCountSort,
                                      String spreadRateSort,
                                      Double spreadRateMin,
                                      Double spreadRateMax) {

        // 목록 조회 조건 반영 후 메모리 페이지 조회
        ProblemStore.ProblemPage problemPage = problemStore.findProblemPage(
                page,
                query,
                solveState,
                currentUserId,
                "asc".equalsIgnoreCase(solvedCountSort),
                spreadRateSort,
                spreadRateMin,
                spreadRateMax
        );

        // 목록 응답 DTO 변환
        List<ProblemListItemRes> problems = problemPage.problems().stream()
                .map(problemEntry -> ProblemListItemRes.of(
                        problemEntry.problem(),
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

        // 문제, 테이블셋 조합으로 상세 응답 구성
        return problemStore.findProblem(problemId)
                .map(problem -> problemStore.findProblemSet(problem.getResolvedProblemSetId())
                        .map(problemSet -> ProblemDetailRes.from(problem, problemSet))
                        .orElseGet(() -> ProblemDetailRes.from(problem)));
    }

    public List<ProblemSetSummaryRes> getProblemSets() {

        // 문제셋 목록 응답 DTO 변환
        return problemStore.findAllProblemSets().stream()
                .map(problemSet -> new ProblemSetSummaryRes(problemSet.getProblemSetId()))
                .toList();
    }

    public Optional<ProblemSetDetailRes> getProblemSet(String problemSetId) {

        // 테이블셋 상세 응답 구성
        return problemStore.findProblemSet(problemSetId)
                .map(ProblemSetDetailRes::from);
    }

    @Transactional
    public ProblemCreateRes createProblem(ProblemCreateReq request) {
        boolean useExistingProblemSet = "existing".equalsIgnoreCase(request.getProblemSetMode());

        // 테이블셋 선택 또는 신규 생성
        ProblemSet problemSet = useExistingProblemSet
                ? findExistingProblemSet(request.getProblemSetId())
                : createProblemSet(request);

        // 다음 문제 번호 계산
        String problemId = createNextProblemId(problemSet.getProblemSetId());

        // 문제 저장
        problemRepository.save(Problem.create(
                problemId,
                problemSet.getProblemSetId(),
                request.getTitle().trim(),
                request.getDescription().trim(),
                normalizeOptionalText(request.getDdlPostgresql()),
                normalizeOptionalText(request.getDdlOracle()),
                request.getCondition().trim(),
                request.getOutput().trim(),
                normalizeOptionalText(request.getOutputSample()),
                normalizeOptionalText(request.getAnswer())
        ));

        problemStore.loadProblems();

        return new ProblemCreateRes(problemId);
    }

    private ProblemSet findExistingProblemSet(String problemSetId) {
        if (problemSetId == null || problemSetId.isBlank()) {
            throw new BusinessException("기존 테이블셋 번호가 필요하다.", HttpStatus.BAD_REQUEST);
        }

        return problemSetRepository.findById(problemSetId.trim())
                .orElseThrow(() -> new BusinessException("존재하지 않는 테이블셋이다.", HttpStatus.NOT_FOUND));
    }

    private ProblemSet createProblemSet(ProblemCreateReq request) {
        String ddlPostgresql = requireText(request.getDdlPostgresql(), "PostgreSQL DDL이 필요하다.");
        String ddlOracle = requireText(request.getDdlOracle(), "Oracle DDL이 필요하다.");
        String dataPostgresql = requireText(request.getDataPostgresql(), "PostgreSQL 데이터 SQL이 필요하다.");
        String dataOracle = requireText(request.getDataOracle(), "Oracle 데이터 SQL이 필요하다.");

        ProblemSet problemSet = ProblemSet.create(
                createNextProblemSetId(),
                ddlPostgresql,
                ddlOracle,
                dataPostgresql,
                dataOracle
        );
        problemSetRepository.save(problemSet);

        return problemSet;
    }

    private String createNextProblemSetId() {
        return problemSetRepository.findAll().stream()
                .map(ProblemSet::getProblemSetId)
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

    private String createNextProblemId(String problemSetId) {
        int nextSequence = problemRepository.findAll().stream()
                .map(Problem::getProblemId)
                .filter(problemId -> problemId.startsWith(problemSetId + "-"))
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

        return problemSetId + "-" + formatFiveDigits(nextSequence);
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
}

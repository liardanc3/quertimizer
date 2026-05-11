package com.quertimizer.problem.application.service;

import com.quertimizer.problem.application.output.ProblemRankingRecordsOutput;
import com.quertimizer.problem.application.output.ProblemRankingSolveRecordOutput;
import com.quertimizer.problem.application.output.ProblemRankingSubmitRecordOutput;
import com.quertimizer.problem.application.port.in.GetProblemRankingRecordsUseCase;
import com.quertimizer.problem.application.port.out.ProblemSolveHistoryRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetProblemRankingRecords implements GetProblemRankingRecordsUseCase {

    private final ProblemSolveHistoryRepositoryPort problemSolveHistoryRepository;
    private final ProblemSubmitHistoryRepositoryPort problemSubmitHistoryRepository;

    /**
     * 랭킹 계산에 필요한 문제 해결과 제출 기록을 조회한다.
     *
     * <ol>
     *   <li>문제 해결 기록 조회와 응답 변환
     *   <li>문제 제출 기록 조회와 응답 변환
     * </ol>
     */
    @Transactional(readOnly = true)
    @Override
    public ProblemRankingRecordsOutput execute() {
        return new ProblemRankingRecordsOutput(
                problemSolveHistoryRepository.findAll().stream().map(this::toSolveRecordOutput).toList(),
                problemSubmitHistoryRepository.findAll().stream().map(this::toSubmitRecordOutput).toList()
        );
    }

    private ProblemRankingSolveRecordOutput toSolveRecordOutput(ProblemSolveHistory history) {
        // 문제 해결 기록을 랭킹 전용 응답으로 변환
        return new ProblemRankingSolveRecordOutput(
                history.getProblemId(), history.getHandle(), history.getDbmsType(),
                history.getExecutionTimeMs(), history.getCost(), history.getSubmittedAt()
        );
    }

    private ProblemRankingSubmitRecordOutput toSubmitRecordOutput(ProblemSubmitHistory history) {
        // 문제 제출 기록을 랭킹 전용 응답으로 변환
        return new ProblemRankingSubmitRecordOutput(history.getHandle(), history.getDbmsType(), history.isSuccess());
    }

}

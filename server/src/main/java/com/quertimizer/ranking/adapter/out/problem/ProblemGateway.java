package com.quertimizer.ranking.adapter.out.problem;

import com.quertimizer.problem.application.output.ProblemRankingSolveRecordOutput;
import com.quertimizer.problem.application.output.ProblemRankingSubmitRecordOutput;
import com.quertimizer.problem.application.port.in.GetProblemRankingRecordsUseCase;
import com.quertimizer.ranking.application.port.out.RankingProblemRecordPort;
import com.quertimizer.ranking.domain.model.RankingSolveRecord;
import com.quertimizer.ranking.domain.model.RankingSubmitRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("rankingProblemGateway")
@RequiredArgsConstructor
public class ProblemGateway implements RankingProblemRecordPort {

    private final GetProblemRankingRecordsUseCase getProblemRankingRecords;

    @Override
    public List<RankingSolveRecord> findSolveRecords() {
        // problem use case 기준 해결 기록 조회 후 ranking 모델 변환
        return getProblemRankingRecords.execute().getSolveRecords().stream()
                .map(this::toSolveRecord)
                .toList();
    }

    @Override
    public List<RankingSubmitRecord> findSubmitRecords() {
        // problem use case 기준 제출 기록 조회 후 ranking 모델 변환
        return getProblemRankingRecords.execute().getSubmitRecords().stream()
                .map(this::toSubmitRecord)
                .toList();
    }

    private RankingSolveRecord toSolveRecord(ProblemRankingSolveRecordOutput output) {
        // problem 해결 기록 응답을 ranking 모델로 변환
        return new RankingSolveRecord(
                output.getProblemId(), output.getHandle(), output.getDbmsType(),
                output.getExecutionTimeMs(), output.getCost(), output.getSubmittedAt()
        );
    }

    private RankingSubmitRecord toSubmitRecord(ProblemRankingSubmitRecordOutput output) {
        // problem 제출 기록 응답을 ranking 모델로 변환
        return new RankingSubmitRecord(output.getHandle(), output.getDbmsType(), output.isSuccess());
    }

}
